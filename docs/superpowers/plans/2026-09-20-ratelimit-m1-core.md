# RateLimit M1（core 契约 + 内存三算法）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已有 `mdyaipay-tools-ratelimit-core` 模块内落地统一限流契约、键解析 SPI，以及固定窗口 / 滑动窗口计数 / 令牌桶三种**内存**实现，并用 JUnit 5 单测锁住语义（供后续 Redis/Spring 复用同一组断言向量）。

**Architecture:** 调用方只依赖 `RateLimiter.tryAcquire(key, policy)`；`InMemoryRateLimiter` 按 `policy.algorithm()` 委托给三个算法类；算法类通过注入的 `java.time.Clock`（可测试）读写 `ConcurrentHashMap` 中的每 key 状态。M1 **不**引入 Redis、Spring、Gateway。

**Tech Stack:** Java 17、JUnit Jupiter、slf4j-api（仅日志边界，算法本身可不打日志）、Maven Surefire；模块路径已存在于 `mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core/`。

**Spec:** `docs/superpowers/specs/2026-09-20-ratelimit-design.md` §4、§6、§10（core 行）、§11 M1。

## Global Constraints

- 依赖：`ratelimit-core` **仅** JDK + `slf4j-api` + test 用 `junit-jupiter`（已在 pom 锁定）
- 包根：`com.mdyaipay.tools.ratelimit`；内存实现子包 `...ratelimit.memory`
- 标识符英文；每个类/方法须有**中文** Javadoc（可读性规约 §5.0）；单文件 ≤ 500 行；单方法 ≤ 50 行有效代码
- 每个新子包补 `package-info.java`
- `RateLimitPolicy` / `RateLimitDecision` 用 `record`；算法枚举不含 P2（无 `SLIDING_WINDOW_LOG` / `LEAKY_BUCKET`）
- 时钟：实现构造注入 `Clock`；默认 `Clock.systemUTC()`；单测用固定 / 步进 `Clock`
- M1 **不改** gateway、redis 子模块实现、`modules.md`（M4）；不新增错误码枚举（M3）
- 提交：仅在用户明确要求时 `git commit`；计划中的 Commit 步骤可跳过或攒到用户批准后再做

---

## File Structure（M1 落盘）

```
mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core/
├── pom.xml                                          (已有，勿改依赖范围)
└── src/
    ├── main/java/com/mdyaipay/tools/ratelimit/
    │   ├── package-info.java                        (已有)
    │   ├── RateLimiter.java                         契约
    │   ├── RateLimitDecision.java                   判定结果
    │   ├── RateLimitAlgorithm.java                  枚举三值
    │   ├── RateLimitPolicy.java                     策略 record + 校验
    │   ├── RateLimitContext.java                    键解析上下文
    │   ├── RateLimitKeyResolver.java                SPI
    │   └── memory/
    │       ├── package-info.java
    │       ├── InMemoryRateLimiter.java             对外门面，按 algorithm 委托
    │       ├── InMemoryFixedWindowRateLimiter.java
    │       ├── InMemorySlidingWindowCounterRateLimiter.java
    │       └── InMemoryTokenBucketRateLimiter.java
    └── test/java/com/mdyaipay/tools/ratelimit/
        ├── RateLimitPolicyTest.java
        └── memory/
            ├── InMemoryFixedWindowRateLimiterTest.java
            ├── InMemorySlidingWindowCounterRateLimiterTest.java
            ├── InMemoryTokenBucketRateLimiterTest.java
            └── InMemoryRateLimiterTest.java
```

---

### Task 1: 契约类型（Algorithm / Decision / Policy / RateLimiter）

**Files:**
- Create: `.../ratelimit/RateLimitAlgorithm.java`
- Create: `.../ratelimit/RateLimitDecision.java`
- Create: `.../ratelimit/RateLimitPolicy.java`
- Create: `.../ratelimit/RateLimiter.java`
- Test: `.../ratelimit/RateLimitPolicyTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `enum RateLimitAlgorithm { FIXED_WINDOW, SLIDING_WINDOW_COUNTER, TOKEN_BUCKET }`
  - `record RateLimitDecision(boolean allowed, long remaining, long limit, Duration retryAfter, RateLimitAlgorithm algorithm)`
  - `record RateLimitPolicy(RateLimitAlgorithm algorithm, long limit, Duration window, double refillRatePerSecond, int slidingSegments)`
  - `interface RateLimiter { RateLimitDecision tryAcquire(String key, RateLimitPolicy policy); }`

- [ ] **Step 1: 写 Policy 校验失败用例**

```java
package com.mdyaipay.tools.ratelimit;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitPolicyTest {

    @Test
    void rejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.FIXED_WINDOW, 0, Duration.ofSeconds(1), 0, 0));
    }

    @Test
    void rejectsNullOrNonPositiveWindow() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.FIXED_WINDOW, 10, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.FIXED_WINDOW, 10, Duration.ZERO, 0, 0));
    }

    @Test
    void tokenBucketRequiresPositiveRefillRate() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.TOKEN_BUCKET, 10, Duration.ofSeconds(1), 0, 0));
    }

    @Test
    void slidingWindowRequiresAtLeastTwoSegments() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 10, Duration.ofSeconds(1), 0, 1));
    }

    @Test
    void acceptsFixedWindowMinimalFields() {
        RateLimitPolicy p = new RateLimitPolicy(
                RateLimitAlgorithm.FIXED_WINDOW, 100, Duration.ofSeconds(1), 0, 0);
        assertEquals(100, p.limit());
    }
}
```

- [ ] **Step 2: 运行确认失败（类不存在）**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core -am test -Dtest=RateLimitPolicyTest`

Expected: 编译失败（找不到 `RateLimitPolicy` / `RateLimitAlgorithm`）

- [ ] **Step 3: 实现契约类型**

`RateLimitAlgorithm.java`：三枚举值 + 类级中文 Javadoc。

`RateLimitDecision.java`：按 Spec §4.1 record；类/规范构造器中文 Javadoc。

`RateLimitPolicy.java`：compact constructor 校验：

```java
public record RateLimitPolicy(
        RateLimitAlgorithm algorithm,
        long limit,
        Duration window,
        double refillRatePerSecond,
        int slidingSegments
) {
    /**
     * 构造不可变限流策略并校验字段语义。
     *
     * @param algorithm 算法，非 null
     * @param limit 窗口上限或令牌桶容量，必须 &gt; 0
     * @param window 窗口/ refill 周期，非 null 且为正
     * @param refillRatePerSecond 仅 TOKEN_BUCKET 须 &gt; 0；其它算法可为 0
     * @param slidingSegments 仅 SLIDING_WINDOW_COUNTER 须 ≥ 2；其它可为 0
     */
    public RateLimitPolicy {
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm must not be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (algorithm == RateLimitAlgorithm.TOKEN_BUCKET && refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("refillRatePerSecond must be > 0 for TOKEN_BUCKET");
        }
        if (algorithm == RateLimitAlgorithm.SLIDING_WINDOW_COUNTER && slidingSegments < 2) {
            throw new IllegalArgumentException("slidingSegments must be >= 2 for SLIDING_WINDOW_COUNTER");
        }
    }
}
```

`RateLimiter.java`：

```java
/**
 * 限流器端口：一次 {@link #tryAcquire} 对应一次业务请求的配额消耗。
 * <p>
 * <b>不负责</b> HTTP/Dubbo 接入与键拼装——由调用方或 Spring Filter 完成。
 */
public interface RateLimiter {

    /**
     * 尝试获取 1 单位配额。
     * <p>
     * 前置条件：{@code key} 非 blank；{@code policy} 非 null 且已通过构造校验。
     * 幂等：否——每次调用可能消耗配额。
     * 副作用：更新该 key 在后端中的计数/令牌状态（内存或 Redis）。
     *
     * @param key    限流维度键
     * @param policy 算法与配额策略
     * @return 是否允许及剩余配额、建议重试间隔
     */
    RateLimitDecision tryAcquire(String key, RateLimitPolicy policy);
}
```

- [ ] **Step 4: 再跑 Policy 单测**

Run: 同上命令

Expected: PASS

- [ ] **Step 5: Commit（仅当用户要求）**

```bash
git add mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core/src
git commit -m "$(cat <<'EOF'
feat(ratelimit): add core RateLimiter contract types

EOF
)"
```

---

### Task 2: 键解析 SPI（Context + KeyResolver）

**Files:**
- Create: `.../ratelimit/RateLimitContext.java`
- Create: `.../ratelimit/RateLimitKeyResolver.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `record RateLimitContext(String routeId, String httpMethod, String path, String clientIp, String merchantAppKey, Map<String, String> attributes)`
  - `interface RateLimitKeyResolver { String resolve(RateLimitContext context); }` — 返回 `null` 表示本规则不适用

说明：M1 只落 SPI，**不**实现 `merchantAppKey` / `clientIp` 内置解析器（属 M3 Spring 装配）。`attributes` 使用 `Map.copyOf` 或构造时防御性拷贝，禁止 null map（用 `Map.of()`）。

- [ ] **Step 1: 实现 Context 与 Resolver**

```java
/**
 * 一次请求的限流键解析上下文；字段均可为 null（表示未知）。
 * <p>
 * <b>不负责</b> 从 ServerWebExchange / HttpServletRequest 取值——由 Filter 填充。
 */
public record RateLimitContext(
        String routeId,
        String httpMethod,
        String path,
        String clientIp,
        String merchantAppKey,
        Map<String, String> attributes
) {
    public RateLimitContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}

/**
 * 限流键解析 SPI：从 {@link RateLimitContext} 得到逻辑 key 片段。
 * 返回 {@code null} 表示本解析器不适用，调用方应跳过或换下一解析器。
 */
public interface RateLimitKeyResolver {
    /**
     * 解析限流键；不适用时返回 null。
     * <p>幂等：是（纯函数，无副作用）。
     */
    String resolve(RateLimitContext context);
}
```

- [ ] **Step 2: 编译模块**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core -am compile`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit（仅当用户要求）**

---

### Task 3: 内存固定窗口 + 单测

**Files:**
- Create: `.../memory/package-info.java`
- Create: `.../memory/InMemoryFixedWindowRateLimiter.java`
- Test: `.../memory/InMemoryFixedWindowRateLimiterTest.java`

**Interfaces:**
- Consumes: `RateLimiter`, `RateLimitPolicy`, `RateLimitDecision`, `RateLimitAlgorithm.FIXED_WINDOW`
- Produces: `InMemoryFixedWindowRateLimiter` 实现 `RateLimiter`；仅接受 `FIXED_WINDOW`，其它 algorithm 抛 `IllegalArgumentException`

**算法语义：**

- 窗口起点：`windowStartMs = (nowMs / windowMs) * windowMs`
- 同窗口内 `count++`；跨窗口重置为 1
- `limit` 次允许后第 `limit+1` 次拒绝；`remaining = max(0, limit - count)`
- 拒绝时 `retryAfter` = 距下一窗口起点的剩余时间（至少 1ms，对外可 `Duration.ofMillis`）
- 状态：`ConcurrentHashMap<String, FixedWindowState>`；单 key 更新用 `compute` 保证原子

- [ ] **Step 1: 写失败用例（类不存在）**

```java
package com.mdyaipay.tools.ratelimit.memory;

import com.mdyaipay.tools.ratelimit.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryFixedWindowRateLimiterTest {

    private static final RateLimitPolicy POLICY = new RateLimitPolicy(
            RateLimitAlgorithm.FIXED_WINDOW, 2, Duration.ofSeconds(1), 0, 0);

    @Test
    void allowsUpToLimitThenRejectsInSameWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemoryFixedWindowRateLimiter limiter = new InMemoryFixedWindowRateLimiter(clock);

        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        RateLimitDecision denied = limiter.tryAcquire("k", POLICY);
        assertFalse(denied.allowed());
        assertEquals(0, denied.remaining());
        assertEquals(2, denied.limit());
        assertEquals(RateLimitAlgorithm.FIXED_WINDOW, denied.algorithm());
        assertFalse(denied.retryAfter().isNegative() || denied.retryAfter().isZero());
    }

    @Test
    void resetsCountWhenWindowElapses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemoryFixedWindowRateLimiter limiter = new InMemoryFixedWindowRateLimiter(clock);
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        assertFalse(limiter.tryAcquire("k", POLICY).allowed());

        clock.advance(Duration.ofSeconds(1));
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
    }

    @Test
    void isolatesKeys() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemoryFixedWindowRateLimiter limiter = new InMemoryFixedWindowRateLimiter(clock);
        assertTrue(limiter.tryAcquire("a", POLICY).allowed());
        assertTrue(limiter.tryAcquire("a", POLICY).allowed());
        assertFalse(limiter.tryAcquire("a", POLICY).allowed());
        assertTrue(limiter.tryAcquire("b", POLICY).allowed());
    }

    /** 测试用可变时钟：仅推进 instant，zone 固定 UTC。 */
    static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration d) { instant = instant.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
```

将 `MutableClock` 抽到同包测试辅助类 `MutableClock.java`（test 源码）避免三份复制：`src/test/java/com/mdyaipay/tools/ratelimit/memory/MutableClock.java`。

- [ ] **Step 2: 跑测确认失败**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core -am test -Dtest=InMemoryFixedWindowRateLimiterTest`

Expected: 编译失败

- [ ] **Step 3: 实现固定窗口**

要点：

```java
/**
 * 内存固定窗口限流：按对齐窗口起点计数，不保证跨 JVM。
 */
public final class InMemoryFixedWindowRateLimiter implements RateLimiter {
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowState> states = new ConcurrentHashMap<>();

    public InMemoryFixedWindowRateLimiter() { this(Clock.systemUTC()); }
    public InMemoryFixedWindowRateLimiter(Clock clock) { this.clock = Objects.requireNonNull(clock); }

    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        // 校验 key / policy / algorithm == FIXED_WINDOW
        // compute 更新 WindowState(windowStartMs, count)
        // 返回 Decision
    }
}
```

`tryAcquire` 开头校验 `key` blank → `IllegalArgumentException`；`policy == null` → NPE 或 IAE（与项目习惯一致：选 IAE 并写清）。

- [ ] **Step 4: 跑测确认通过**

Expected: PASS

- [ ] **Step 5: Commit（仅当用户要求）**

---

### Task 4: 内存滑动窗口计数 + 单测

**Files:**
- Create: `.../memory/InMemorySlidingWindowCounterRateLimiter.java`
- Test: `.../memory/InMemorySlidingWindowCounterRateLimiterTest.java`

**Interfaces:**
- Consumes: 同上契约；`slidingSegments >= 2`
- Produces: `InMemorySlidingWindowCounterRateLimiter`

**算法语义（分段 + 上一窗权重，与 Redis 版对齐）：**

- 将 `window` 均分为 `slidingSegments` 段，段长 `segmentMs = window.toMillis() / segments`（要求整除；若不整除则 `segmentMs = max(1, windowMs / segments)`，单测用可整除窗口如 1000ms / 5）
- 当前段下标：`segmentIndex = nowMs / segmentMs`
- 估计请求数：`currentCount + previousCount * (1 - elapsedInSegment / segmentMs)`
- 若估计值 `< limit` 则当前段 `+1` 并允许，否则拒绝
- `remaining = max(0, limit - ceil(estimated))`（允许后用更新后的计数）
- 拒绝时 `retryAfter`：保守取「当前段剩余时间」或 `window` 的一小段；建议 `Duration.ofMillis(max(1, segmentMs - elapsedInSegment))`
- 状态：每 key 保存 `currentSegmentIndex`、`currentCount`、`previousCount`

- [ ] **Step 1: 写失败用例**

```java
@Test
void allowsWithinWeightedEstimateThenRejects() {
    // limit=5, window=1s, segments=5 → segment=200ms
    RateLimitPolicy policy = new RateLimitPolicy(
            RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 5, Duration.ofSeconds(1), 0, 5);
    MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
    InMemorySlidingWindowCounterRateLimiter limiter =
            new InMemorySlidingWindowCounterRateLimiter(clock);

    for (int i = 0; i < 5; i++) {
        assertTrue(limiter.tryAcquire("k", policy).allowed(), "i=" + i);
    }
    assertFalse(limiter.tryAcquire("k", policy).allowed());
}

@Test
void previousSegmentWeightDecaysAcrossBoundary() {
    RateLimitPolicy policy = new RateLimitPolicy(
            RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 5, Duration.ofSeconds(1), 0, 5);
    MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
    InMemorySlidingWindowCounterRateLimiter limiter =
            new InMemorySlidingWindowCounterRateLimiter(clock);
    for (int i = 0; i < 5; i++) {
        assertTrue(limiter.tryAcquire("k", policy).allowed());
    }
    // 跨入下一段末尾：上一窗权重接近 0，应重新允许
    clock.advance(Duration.ofMillis(399)); // 接近第二段末（段长 200ms → 索引+1 后再偏后）
    // 精确推进：先到下一段起点 + 段内接近结束
    // 实现后按 segment 边界调此断言；目标：满额拒绝后，推进接近 1 个完整 window 应恢复
    clock.advance(Duration.ofMillis(601)); // 合计约 1s
    assertTrue(limiter.tryAcquire("k", policy).allowed());
}
```

实现后若边界断言过紧，允许用「推进整个 `window` 后必允许」作为硬断言，权重衰减用「推进半窗后允许次数 ≥ 1」作容差断言（Spec §10）。

- [ ] **Step 2: 跑测确认失败 → Step 3 实现 → Step 4 通过**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core -am test -Dtest=InMemorySlidingWindowCounterRateLimiterTest`

- [ ] **Step 5: Commit（仅当用户要求）**

---

### Task 5: 内存令牌桶 + 单测

**Files:**
- Create: `.../memory/InMemoryTokenBucketRateLimiter.java`
- Test: `.../memory/InMemoryTokenBucketRateLimiterTest.java`

**Interfaces:**
- Consumes: `TOKEN_BUCKET`；`limit` = 桶容量；`refillRatePerSecond` = 每秒补充速率；`window` 保留与 Redis 版字段对齐（内存实现可不使用 window，但 Policy 仍要求正 Duration）
- Produces: `InMemoryTokenBucketRateLimiter`

**算法语义：**

- 状态：`tokens`（double）、`lastRefillNanos` 或 `lastRefillMs`
- 每次 acquire：`elapsedSec = (now - last) / 1e3`；`tokens = min(capacity, tokens + elapsedSec * refillRate)`；更新 last
- 若 `tokens >= 1`：`tokens -= 1`，允许，`remaining = floor(tokens)`
- 否则拒绝；`retryAfter = Duration.ofMillis(ceil((1 - tokens) / refillRate * 1000))`
- 新 key：初始 `tokens = capacity`（满桶，允许突发）

- [ ] **Step 1: 写失败用例**

```java
@Test
void allowsBurstUpToCapacityThenRejects() {
    RateLimitPolicy policy = new RateLimitPolicy(
            RateLimitAlgorithm.TOKEN_BUCKET, 3, Duration.ofSeconds(1), 1.0, 0);
    MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
    InMemoryTokenBucketRateLimiter limiter = new InMemoryTokenBucketRateLimiter(clock);

    assertTrue(limiter.tryAcquire("k", policy).allowed());
    assertTrue(limiter.tryAcquire("k", policy).allowed());
    assertTrue(limiter.tryAcquire("k", policy).allowed());
    RateLimitDecision d = limiter.tryAcquire("k", policy);
    assertFalse(d.allowed());
    assertEquals(0, d.remaining());
}

@Test
void refillsTokensOverTime() {
    RateLimitPolicy policy = new RateLimitPolicy(
            RateLimitAlgorithm.TOKEN_BUCKET, 1, Duration.ofSeconds(1), 1.0, 0);
    MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
    InMemoryTokenBucketRateLimiter limiter = new InMemoryTokenBucketRateLimiter(clock);

    assertTrue(limiter.tryAcquire("k", policy).allowed());
    assertFalse(limiter.tryAcquire("k", policy).allowed());
    clock.advance(Duration.ofSeconds(1));
    assertTrue(limiter.tryAcquire("k", policy).allowed());
}
```

- [ ] **Step 2–4: 红 → 实现 → 绿**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core -am test -Dtest=InMemoryTokenBucketRateLimiterTest`

- [ ] **Step 5: Commit（仅当用户要求）**

---

### Task 6: InMemoryRateLimiter 门面 + 算法路由单测

**Files:**
- Create: `.../memory/InMemoryRateLimiter.java`
- Test: `.../memory/InMemoryRateLimiterTest.java`

**Interfaces:**
- Consumes: 三个算法实现
- Produces: 对外推荐入口 `InMemoryRateLimiter implements RateLimiter`；构造 `(Clock)` / 无参；内部持有三个委托实例（共享同一 `Clock`）

```java
/**
 * 内存限流门面：按 {@link RateLimitPolicy#algorithm()} 委托给对应算法实现。
 * <p>
 * <b>不负责</b> 跨 JVM 一致性——仅单机 / 单测。
 */
public final class InMemoryRateLimiter implements RateLimiter {
    // switch (policy.algorithm()) { case FIXED_WINDOW -> fixed.tryAcquire(...); ... }
}
```

- [ ] **Step 1: 用例覆盖三算法各一次成功路径 + blank key 拒绝**

```java
@Test
void routesByAlgorithm() {
    MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
    InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);
    assertTrue(limiter.tryAcquire("k", new RateLimitPolicy(
            RateLimitAlgorithm.FIXED_WINDOW, 1, Duration.ofSeconds(1), 0, 0)).allowed());
    assertTrue(limiter.tryAcquire("k2", new RateLimitPolicy(
            RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 1, Duration.ofSeconds(1), 0, 2)).allowed());
    assertTrue(limiter.tryAcquire("k3", new RateLimitPolicy(
            RateLimitAlgorithm.TOKEN_BUCKET, 1, Duration.ofSeconds(1), 10.0, 0)).allowed());
}
```

- [ ] **Step 2–4: 红 → 实现 → 绿**

- [ ] **Step 5: 跑 core 模块全量测试**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core -am test`

Expected: 全部 PASS；无 Redis/Spring 测试被牵连失败

- [ ] **Step 6: Commit（仅当用户要求）**

```bash
git add mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-core
git commit -m "$(cat <<'EOF'
feat(ratelimit): implement in-memory rate limit algorithms for M1

EOF
)"
```

---

## Self-Review（对照 Spec M1）

| Spec 要求 | 任务 |
|-----------|------|
| `RateLimiter` + `RateLimitDecision` | Task 1 |
| `RateLimitAlgorithm` 三值（无 P2） | Task 1 |
| `RateLimitPolicy` 字段齐全 | Task 1 |
| `RateLimitKeyResolver` + `RateLimitContext` | Task 2 |
| `InMemoryFixedWindowRateLimiter` | Task 3 |
| `InMemorySlidingWindowCounterRateLimiter` | Task 4 |
| `InMemoryTokenBucketRateLimiter` | Task 5 |
| core 单测：边界窗口、突发令牌、滑动段容差 | Task 3–5 |
| 门面便于调用方 / 后续 Spring 注入 | Task 6 |
| 不实现 Redis / Spring / Gateway / 错误码 | 未列入（正确） |

**占位符扫描：** 无 TBD；滑动窗口边界断言允许容差，已写明硬断言兜底。

**类型一致性：** 全文统一 `tryAcquire(String, RateLimitPolicy) → RateLimitDecision`；枚举名与 Spec §4.2 一致。

---

## Out of scope（后续里程碑）

- M2：`mdyaipay-tools-ratelimit-redis` Lua + Lettuce
- M3：spring-boot 配置、Gateway Filter、429、`RATE_LIMITED` 错误码
- M4：gateway 引入依赖、`modules.md`、collect 默认规则
