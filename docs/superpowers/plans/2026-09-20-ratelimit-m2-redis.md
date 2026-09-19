# RateLimit M2（Redis + Lua 三算法）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `mdyaipay-tools-ratelimit-redis` 落地 Lettuce + Lua 的固定窗口 / 滑动窗口计数 / 令牌桶，语义与 M1 内存实现对齐，对外仍只暴露 `RateLimiter`。

**Architecture:** `RedisRateLimiter` 门面按 `policy.algorithm()` 选脚本；`RedisScriptSupport` 负责 `SCRIPT LOAD` + `EVALSHA`（NOSCRIPT 时降级 `EVAL`）；时钟一律取自 Lua 内 `redis.call('TIME')`。Key：`mdyaipay:rl:{algorithm}:{logicalKey}`。

**Tech Stack:** Java 17、Lettuce（版本由根 `spring-boot-dependencies` 3.5.6 BOM 管理）、JUnit 5；集成测连本地 **`redis7:6379`**（已在跑；不可用则 `Assumptions.assumeTrue` 跳过）。**不**引入 Testcontainers（YAGNI：环境已有 redis7）。

**Spec:** `docs/superpowers/specs/2026-09-20-ratelimit-design.md` §5、§10（redis 行）、§11 M2。  
**依赖契约：** M1 已交付的 `RateLimiter` / `RateLimitPolicy` / `RateLimitDecision` / `RateLimitAlgorithm`。

## Global Constraints

- 依赖：`ratelimit-core` + `lettuce-core` + test `junit-jupiter`；**不得**依赖 Spring / Gateway / common-core
- 包根：`com.mdyaipay.tools.ratelimit.redis`
- 中文 Javadoc；单文件 ≤ 500 行；单方法 ≤ 50 行有效代码；新子包补 `package-info.java`
- Lua 返回值统一：`{allowed(0|1), remaining, limit, retryAfterMs}`（Integer 列表），Java 解析为 `RateLimitDecision`
- 语义对齐 M1：固定窗口**时钟对齐**；滑动窗口「当前段 + 上一段权重」；令牌桶满桶突发 + `refillRatePerSecond`
- M2 **不**做 Spring 自动配置、Gateway Filter、错误码（M3）；不改 `modules.md`（M4）
- 提交：仅用户明确要求时 commit

---

## File Structure（M2 落盘）

```
mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-redis/
├── pom.xml                                              (已有；按需补 slf4j-api)
└── src/
    ├── main/
    │   ├── java/com/mdyaipay/tools/ratelimit/redis/
    │   │   ├── package-info.java                        (已有)
    │   │   ├── RedisKeyNames.java                       key 拼装
    │   │   ├── RedisScriptSupport.java                  LOAD / EVALSHA / EVAL
    │   │   ├── RedisLuaResult.java                      解析 Lua 四元组
    │   │   ├── RedisRateLimiter.java                    门面 implements RateLimiter
    │   │   └── script/
    │   │       ├── package-info.java
    │   │       └── RateLimitScripts.java                枚举：classpath 资源名
    │   └── resources/ratelimit/lua/
    │       ├── fixed_window.lua
    │       ├── sliding_window_counter.lua
    │       └── token_bucket.lua
    └── test/java/com/mdyaipay/tools/ratelimit/redis/
        ├── RedisIntegrationSupport.java                 连接假设 + 唯一 key 前缀
        ├── RedisFixedWindowRateLimiterIT.java
        ├── RedisSlidingWindowCounterRateLimiterIT.java
        ├── RedisTokenBucketRateLimiterIT.java
        ├── RedisRateLimiterIT.java
        └── RedisRateLimiterConcurrencyIT.java           32 线程不超卖
```

---

### Task 1: 基础设施（Key / Lua 结果 / ScriptSupport / 连接假设）

**Files:**
- Create: `RedisKeyNames.java`, `RedisLuaResult.java`, `RedisScriptSupport.java`, `script/RateLimitScripts.java`, `script/package-info.java`
- Create: `src/main/resources/ratelimit/lua/*.lua`（先放固定窗口脚本桩，后续 Task 填满）
- Test: `RedisIntegrationSupport.java`

**Interfaces:**
- Consumes: Lettuce `StatefulRedisConnection<String, String>` / `RedisCommands`
- Produces:
  - `RedisKeyNames.of(RateLimitAlgorithm algorithm, String logicalKey) → "mdyaipay:rl:{algo}:{logicalKey}"`
  - `RedisLuaResult.parse(List<?>) → record(allowed, remaining, limit, retryAfterMs)`
  - `RedisScriptSupport.eval(RateLimitScripts script, String key, String... argv) → RedisLuaResult`
  - `RateLimitScripts` 枚举含 `resourcePath()` / `algorithm()`

- [ ] **Step 1: 写 Key 与结果解析单测（纯单元，不需 Redis）**

在 `src/test/java/.../RedisKeyNamesTest.java`：

```java
@Test
void buildsPrefixedKey() {
    assertEquals(
            "mdyaipay:rl:TOKEN_BUCKET:merchant:mk_1",
            RedisKeyNames.of(RateLimitAlgorithm.TOKEN_BUCKET, "merchant:mk_1"));
}

@Test
void parsesLuaTuple() {
    RedisLuaResult r = RedisLuaResult.parse(List.of(1L, 9L, 10L, 0L));
    assertTrue(r.allowed());
    assertEquals(9, r.remaining());
    assertEquals(10, r.limit());
    assertEquals(0, r.retryAfterMs());
}
```

- [ ] **Step 2: 跑测确认失败（类不存在）**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-redis -am test -Dtest=RedisKeyNamesTest`

- [ ] **Step 3: 实现 Key / Result / ScriptSupport**

`RedisScriptSupport` 要点：

```java
/**
 * Lua 脚本装载与执行：优先 EVALSHA，遇 NOSCRIPT 降级 EVAL 并刷新 SHA。
 */
public final class RedisScriptSupport implements AutoCloseable {
    // 构造注入 StatefulRedisConnection；构造时 SCRIPT LOAD 三个脚本
    // eval(script, key, argv...): sync().evalsha / catch → eval(scriptBody)
}
```

`RedisIntegrationSupport`（test）：

```java
static final String REDIS_URI = System.getenv().getOrDefault(
        "MDYAIPAY_RATELIMIT_REDIS_URI", "redis://127.0.0.1:6379");

static StatefulRedisConnection<String, String> openOrSkip() {
    try {
        RedisClient client = RedisClient.create(REDIS_URI);
        StatefulRedisConnection<String, String> c = client.connect();
        c.sync().ping();
        return c;
    } catch (Exception e) {
        Assumptions.assumeTrue(false, "Redis unavailable at " + REDIS_URI + ": " + e.getMessage());
        return null;
    }
}
```

每个 IT 用唯一 logicalKey：`"it-" + UUID`，`@AfterEach` 可 `DEL` 相关 key（或依赖 TTL）。

- [ ] **Step 4: 绿测 KeyNamesTest + 编译 redis 模块**

Run: `mvn -pl .../mdyaipay-tools-ratelimit-redis -am test -Dtest=RedisKeyNamesTest`

- [ ] **Step 5: Commit（仅用户要求时）**

---

### Task 2: 固定窗口 Lua + RedisRateLimiter 首通路径

**Files:**
- Create/Modify: `resources/ratelimit/lua/fixed_window.lua`
- Create: `RedisRateLimiter.java`（先只路由 FIXED_WINDOW，其它算法抛 IAE，后续 Task 补全）
- Test: `RedisFixedWindowRateLimiterIT.java`

**Lua 语义（对齐 M1 时钟对齐窗口）：**

```lua
-- KEYS[1]=redisKey  ARGV[1]=limit  ARGV[2]=windowMs
local t = redis.call('TIME')
local nowMs = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
local limit = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local windowStart = math.floor(nowMs / windowMs) * windowMs
local bucketKey = KEYS[1] .. ':' .. tostring(windowStart)
local count = redis.call('INCR', bucketKey)
if count == 1 then
  redis.call('PEXPIRE', bucketKey, windowMs)
end
if count <= limit then
  return {1, limit - count, limit, 0}
end
local ttl = redis.call('PTTL', bucketKey)
if ttl < 1 then ttl = 1 end
return {0, 0, limit, ttl}
```

**Java 调用：** `ARGV` = `limit`, `window.toMillis()`；`algorithm` 写入 Decision。

- [ ] **Step 1: 写 IT（需 Redis）**

```java
@Test
void allowsUpToLimitThenRejects() {
    var conn = RedisIntegrationSupport.openOrSkip();
    try (RedisRateLimiter limiter = RedisRateLimiter.create(conn)) {
        RateLimitPolicy p = new RateLimitPolicy(
                RateLimitAlgorithm.FIXED_WINDOW, 2, Duration.ofSeconds(1), 0, 0);
        String key = "fw-" + UUID.randomUUID();
        assertTrue(limiter.tryAcquire(key, p).allowed());
        assertTrue(limiter.tryAcquire(key, p).allowed());
        RateLimitDecision d = limiter.tryAcquire(key, p);
        assertFalse(d.allowed());
        assertEquals(0, d.remaining());
        assertTrue(d.retryAfter().toMillis() > 0);
    }
}
```

`RedisRateLimiter.create(connection)`：**不**关闭外部注入的 connection（Spring 场景）；另提供 `create(RedisURI)` 拥有 client+connection 并在 `close()` 释放。

推荐构造：

```java
public final class RedisRateLimiter implements RateLimiter, AutoCloseable {
    public RedisRateLimiter(StatefulRedisConnection<String, String> connection, boolean closeConnection) { ... }
    public static RedisRateLimiter wrap(StatefulRedisConnection<String, String> connection) {
        return new RedisRateLimiter(connection, false);
    }
}
```

IT 用 `wrap(openOrSkip())`，在 `@AfterAll` / try-with-resources 关 connection。

- [ ] **Step 2: 红（类/脚本不存在）→ Step 3 实现 → Step 4 绿**

Run: `mvn -pl .../mdyaipay-tools-ratelimit-redis -am test -Dtest=RedisFixedWindowRateLimiterIT`

Expected: PASS（本机 redis7 可用时）

- [ ] **Step 5: Commit（仅用户要求时）**

---

### Task 3: 滑动窗口计数 Lua

**Files:**
- Create: `resources/ratelimit/lua/sliding_window_counter.lua`
- Modify: `RedisRateLimiter` 路由 `SLIDING_WINDOW_COUNTER`
- Test: `RedisSlidingWindowCounterRateLimiterIT.java`

**Lua 语义（对齐 M1 `InMemorySlidingWindowCounterRateLimiter`）：**

Hash 字段：`seg`（当前段下标）、`cur`、`prev`。

```lua
-- ARGV: limit, windowMs, segments
local t = redis.call('TIME')
local nowMs = ...
local limit, windowMs, segments = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
local segmentMs = math.max(1, math.floor(windowMs / segments))
local seg = math.floor(nowMs / segmentMs)
local elapsed = nowMs % segmentMs
local key = KEYS[1]
local curSeg = tonumber(redis.call('HGET', key, 'seg') or '-1')
local cur = tonumber(redis.call('HGET', key, 'cur') or '0')
local prev = tonumber(redis.call('HGET', key, 'prev') or '0')
if curSeg ~= seg then
  local gap = seg - curSeg
  if gap == 1 then prev = cur else prev = 0 end
  cur = 0
  curSeg = seg
end
local weight = 1.0 - (elapsed / segmentMs)
local estimated = cur + prev * weight
if estimated < limit then
  cur = cur + 1
  redis.call('HMSET', key, 'seg', curSeg, 'cur', cur, 'prev', prev)
  redis.call('PEXPIRE', key, windowMs * 2)
  local rem = math.max(0, limit - math.ceil(estimated + 1))
  return {1, rem, limit, 0}
end
local retry = math.max(1, segmentMs - elapsed)
redis.call('HMSET', key, 'seg', curSeg, 'cur', cur, 'prev', prev)
redis.call('PEXPIRE', key, windowMs * 2)
return {0, 0, limit, retry}
```

- [ ] **Step 1: IT — 同段满额拒绝；推进约 1s 后恢复（用 `Thread.sleep` 或第二次连接等待真实 TIME）**

说明：Redis 用真实时钟，**不能**注入 `MutableClock`。断言策略：

1. `limit=5` 连打 5 次允许、第 6 次拒绝（同秒内完成）
2. `Thread.sleep(1100)` 后再次允许

- [ ] **Step 2–4: 红 → 实现 → 绿**

Run: `mvn -pl .../mdyaipay-tools-ratelimit-redis -am test -Dtest=RedisSlidingWindowCounterRateLimiterIT`

---

### Task 4: 令牌桶 Lua

**Files:**
- Create: `resources/ratelimit/lua/token_bucket.lua`
- Modify: `RedisRateLimiter` 路由 `TOKEN_BUCKET`
- Test: `RedisTokenBucketRateLimiterIT.java`

**Lua 语义（对齐 M1）：**

```lua
-- ARGV: capacity, refillRatePerSecond
-- Hash: tokens, ts (lastRefillMs)
local t = redis.call('TIME')
local nowMs = ...
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local key = KEYS[1]
local tokens = tonumber(redis.call('HGET', key, 'tokens'))
local ts = tonumber(redis.call('HGET', key, 'ts'))
if tokens == nil then
  tokens = capacity
  ts = nowMs
else
  local elapsed = math.max(0, nowMs - ts)
  tokens = math.min(capacity, tokens + (elapsed / 1000.0) * rate)
  ts = nowMs
end
if tokens >= 1 then
  tokens = tokens - 1
  redis.call('HMSET', key, 'tokens', tokens, 'ts', ts)
  redis.call('PEXPIRE', key, math.max(1000, math.ceil(capacity / rate * 2000)))
  return {1, math.floor(tokens), capacity, 0}
end
local deficit = 1 - tokens
local retry = math.max(1, math.ceil((deficit / rate) * 1000))
redis.call('HMSET', key, 'tokens', tokens, 'ts', ts)
redis.call('PEXPIRE', key, ...)
return {0, 0, capacity, retry}
```

- [ ] **Step 1: IT — 容量 3 突发后拒绝；`sleep(1100)` + `rate=1` 后再允许**

- [ ] **Step 2–4: 红 → 实现 → 绿**

---

### Task 5: 门面补全 + 并发不超卖

**Files:**
- Modify: `RedisRateLimiter.java`（三算法 switch 完整；blank key / null policy 校验）
- Test: `RedisRateLimiterIT.java`（三算法各一次成功）
- Test: `RedisRateLimiterConcurrencyIT.java`

**并发用例（Spec §10）：**

```java
@Test
void thirtyTwoThreadsDoNotOversellFixedWindow() throws Exception {
    // limit=100, window=60s；32 线程各尝试 20 次 → 最多 100 次 allowed
    int limit = 100;
    AtomicInteger allowed = new AtomicInteger();
    // CountDownLatch + 32 threads
    assertEquals(limit, allowed.get());
}
```

令牌桶也可做：`capacity=50`，瞬时并发，`allowed == 50`。

- [ ] **Step 1: 写并发测与路由测**
- [ ] **Step 2–4: 实现缺口 → 全绿**
- [ ] **Step 5: 模块全量测试**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-redis -am test`

Expected: BUILD SUCCESS；Redis 不可用时 IT 以 Assumption 跳过（报告 Skipped，非 Failure）

- [ ] **Step 6: Commit（仅用户要求时）**

```bash
git commit -m "$(cat <<'EOF'
feat(ratelimit): M2 Redis Lua drivers for three algorithms

EOF
)"
```

---

## Self-Review（对照 Spec M2）

| Spec 要求 | 任务 |
|-----------|------|
| Key `mdyaipay:rl:{algorithm}:{logicalKey}` | Task 1 |
| 固定窗口 INCR + 窗口对齐 | Task 2 |
| 滑动窗口 Hash 分段加权 | Task 3 |
| 令牌桶 Hash tokens+ts，Redis TIME | Task 4 |
| EVALSHA + SCRIPT LOAD，失败 EVAL | Task 1 / 全流程 |
| Lettuce 连接注入 / 工厂 | Task 2 |
| 并发 32 线程不超卖 | Task 5 |
| 不引入 Spring / Gateway | 未列入 |

**占位符扫描：** 无 TBD；真实时钟 IT 用 `Thread.sleep` 已写明。

**与 M1 差异（有意）：** Redis 侧不可注入 `Clock`；IT 依赖墙钟与本地 redis7。

---

## Out of scope

- M3：spring-boot 配置、Gateway Filter、429、错误码
- M4：gateway 引入、`modules.md`
- Testcontainers、Cluster hash tag、fail-open 策略（属 M3 配置）
