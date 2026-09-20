# RateLimit M3（spring-boot 配置 + Gateway Filter + 429）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `mdyaipay-tools-ratelimit-spring-boot` 落地 `mdyaipay.ratelimit.*` 配置绑定、内存/Redis 后端装配、Gateway `GlobalFilter`（429 + 响应头），并扩展 `ErrorCode`；可选 Servlet Filter **默认关闭**。

**Architecture:** `RateLimitProperties` → 装配 `RateLimiter`（memory / redis）→ `RateLimitGatewayFilter` 匹配首条 rule → `tryAcquire`；拒绝写 `ApiResponse.fail(RATE_LIMITED)` + `Retry-After` / `X-RateLimit-*`。后端异常按 `fail-open` 分支。M3 **不**改 `mdyaipay-gateway` 的 `pom`/`application.yml`（属 M4）。

**Tech Stack:** Spring Boot 3.5 自动配置、Spring Cloud Gateway（optional）、Lettuce（经 optional redis 模块）、`mdyaipay-tools-common-core`（optional，错误码/ApiResponse）、JUnit 5 + spring-boot-test。

**Spec:** `docs/superpowers/specs/2026-09-20-ratelimit-design.md` §7、§9、§11 M3。依赖 M1/M2 契约与 `RedisRateLimiter`。

## Global Constraints

- Gateway Filter **Order = `Ordered.HIGHEST_PRECEDENCE + 5`**：在 Trace（`HIGHEST`）之后、`MerchantSignedCollectGatewayFilter`（`+10`）之前（collect 不继续 chain，限流必须更靠前）
- `merchantAppKey` 解析：**不读 body**（避免与 collect 抢 body）；顺序：Exchange attribute `mdyaipay.ratelimit.merchantAppKey` → Header `X-App-Key` → Query `appKey`；取不到则该 resolver 返回 null，组合键跳过该段
- 组合键：`key-resolvers` 列表非 null 片段用 `:` 连接；若结果 blank → **跳过本条 rule**（不限流）
- path 匹配：Ant 风格（`PathPatternParser` / `AntPathMatcher`）；methods 空 = 任意方法
- `backend=redis` 且无 Lettuce/连接失败：遵循 `fail-open`（默认 `false` → 503 + `RATE_LIMIT_BACKEND_UNAVAILABLE`）
- 中文 Javadoc；单文件 ≤ 500 行；单方法 ≤ 50 行有效代码
- **不**实现 Dubbo Filter（P2）；**不**改 gateway 业务代码（M4）
- 提交仅用户明确要求时

---

## File Structure

```
mdyaipay-tools-common/.../ErrorCode.java          增加 RATE_LIMITED、RATE_LIMIT_BACKEND_UNAVAILABLE

mdyaipay-tools-ratelimit-spring-boot/
├── pom.xml                                       补 spring-boot-starter-test、jackson（test/optional）
└── src/main/
    ├── java/com/mdyaipay/tools/ratelimit/
    │   ├── autoconfigure/
    │   │   ├── package-info.java                 (已有)
    │   │   ├── RateLimitProperties.java
    │   │   ├── RateLimitAutoConfiguration.java
    │   │   ├── RateLimitGatewayAutoConfiguration.java
    │   │   └── RateLimitServletAutoConfiguration.java
    │   ├── gateway/
    │   │   ├── package-info.java
    │   │   ├── RateLimitGatewayFilter.java
    │   │   └── RateLimitDeniedWriter.java        写 429/503 JSON + 头
    │   ├── servlet/
    │   │   ├── package-info.java
    │   │   └── RateLimitServletFilter.java       默认不启用
    │   ├── match/
    │   │   ├── package-info.java
    │   │   ├── RateLimitRuleMatcher.java
    │   │   └── RateLimitKeyComposer.java
    │   └── resolve/
    │       ├── package-info.java
    │       ├── ClientIpKeyResolver.java
    │       ├── PathKeyResolver.java
    │       ├── RouteIdKeyResolver.java
    │       └── MerchantAppKeyResolver.java
    └── resources/META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports

src/test/java/.../
    ├── RateLimitRuleMatcherTest.java
    ├── RateLimitKeyComposerTest.java
    ├── RateLimitGatewayFilterTest.java           Mock RateLimiter → 429
    └── RateLimitAutoConfigurationMemoryTest.java @SpringBootTest memory
```

---

### Task 1: ErrorCode 扩展

**Files:**
- Modify: `mdyaipay-tools/mdyaipay-tools-common/mdyaipay-tools-common-core/src/main/java/com/mdyaipay/tools/exception/ErrorCode.java`

**Produces:**
- `RATE_LIMITED(42900, "rate limited")`
- `RATE_LIMIT_BACKEND_UNAVAILABLE(50301, "rate limit backend unavailable")`

说明：HTTP 状态仍由 Filter 设 429/503；业务码用枚举 `getCode()`。

- [ ] **Step 1: 写断言单测（若 common-core 已有 ErrorCode 测则追加；否则新建 `ErrorCodeRateLimitTest`）**

```java
@Test
void rateLimitCodesDefined() {
    assertEquals(42900, ErrorCode.RATE_LIMITED.getCode());
    assertEquals(50301, ErrorCode.RATE_LIMIT_BACKEND_UNAVAILABLE.getCode());
}
```

- [ ] **Step 2: 红 → Step 3 加枚举值 → Step 4 绿**

---

### Task 2: Properties + Rule 匹配 + Key 组合

**Files:**
- Create: `RateLimitProperties.java`（嵌套 `Redis`、`Policy`、`Rule`、`Match`）
- Create: `match/RateLimitRuleMatcher.java`、`RateLimitKeyComposer.java`
- Create: `resolve/*KeyResolver.java`（实现 core `RateLimitKeyResolver`）
- Test: `RateLimitRuleMatcherTest`、`RateLimitKeyComposerTest`

**Properties 形状（对齐 Spec §7.1）：**

```java
@ConfigurationProperties(prefix = "mdyaipay.ratelimit")
public class RateLimitProperties {
    private boolean enabled = true;
    private String backend = "memory"; // memory | redis
    private boolean failOpen = false;
    private Redis redis = new Redis();
    private PolicySpec defaultPolicy;
    private List<RuleSpec> rules = List.of();
    // getters/setters；PolicySpec 字段：algorithm, limit, window, refillRatePerSecond, slidingSegments
    // RuleSpec：id, match, policy, keyResolvers
    // MatchSpec：path, methods
}
```

`window` 用 `Duration`（Boot 绑定 `1s`）。

`RateLimitRuleMatcher.firstMatch(rules, method, path)` → `Optional<RuleSpec>`。

`RateLimitKeyComposer.compose(resolversByName, names, context)` → `Optional<String>`。

内置 resolver bean 名：`clientIp`、`path`、`routeId`、`merchantAppKey`（与 yaml `key-resolvers` 字符串一致）。

- [ ] **Step 1: Matcher/Composer 单测（无 Spring）**
- [ ] **Step 2–4: 红 → 实现 → 绿**

---

### Task 3: AutoConfiguration 装配 RateLimiter

**Files:**
- Create: `RateLimitAutoConfiguration.java`
- Create: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `RateLimitAutoConfigurationMemoryTest`

**行为：**

```java
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "mdyaipay.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnProperty(prefix = "mdyaipay.ratelimit", name = "backend", havingValue = "memory", matchIfMissing = true)
    RateLimiter inMemoryRateLimiter() { return new InMemoryRateLimiter(); }

    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnProperty(prefix = "mdyaipay.ratelimit", name = "backend", havingValue = "redis")
    @ConditionalOnClass(name = "com.mdyaipay.tools.ratelimit.redis.RedisRateLimiter")
    RateLimiter redisRateLimiter(RateLimitProperties props) {
        // RedisClient.create(props.getRedis().getUri()); wrap connection
        // Bean 销毁时关闭 client/connection —— 用 @Bean(destroyMethod) 或 DisposableBean 包装
    }

    @Bean Map<String, RateLimitKeyResolver> builtInResolvers() { ... }
}
```

Redis 连接包装建议独立小类 `RedisRateLimiterHolder implements RateLimiter, DisposableBean`，避免泄漏。

- [ ] **Step 1: `@SpringBootTest` + `backend=memory` 注入 `RateLimiter` 为 `InMemoryRateLimiter`**
- [ ] **Step 2–4: 红 → 实现 → 绿**

pom 测试依赖：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

---

### Task 4: Gateway Filter + 429 写入

**Files:**
- Create: `gateway/RateLimitGatewayFilter.java`、`RateLimitDeniedWriter.java`
- Create: `RateLimitGatewayAutoConfiguration.java`
- Test: `RateLimitGatewayFilterTest`

**Filter 流程：**

1. `enabled` 且 `rules` 非空，否则 `chain.filter`
2. 从 `ServerWebExchange` 建 `RateLimitContext`（path、method、clientIp、routeId from `GATEWAY_PREDICATE_PATH_ATTR` / request；merchantAppKey 按 Task2 规则）
3. `firstMatch` → 无匹配则放行
4. `compose` key → empty 则放行
5. `policy`：rule.policy 缺省字段合并 `defaultPolicy`
6. `tryAcquire`；允许则写可选响应头（可用 `beforeCommit`）并 `chain.filter`
7. 拒绝：`RateLimitDeniedWriter.write429(exchange, decision)`
8. catch 后端异常：`failOpen` ? log WARN + chain.filter : write503

**DeniedWriter：**

- Status 429 / 503
- Body：`ApiResponse.fail(ErrorCode.RATE_LIMITED)`（需 `@ConditionalOnClass(ApiResponse)`；若无 common，降级纯文本 `"rate limited"`）
- Headers：`Retry-After`（秒，ceil）、`X-RateLimit-Limit`、`X-RateLimit-Remaining`

**Order：** `Ordered.HIGHEST_PRECEDENCE + 5`。

Gateway 自动配置：

```java
@AutoConfiguration(after = RateLimitAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@ConditionalOnProperty(...)
class RateLimitGatewayAutoConfiguration {
  @Bean RateLimitGatewayFilter rateLimitGatewayFilter(...) { ... }
}
```

- [ ] **Step 1: 单测 Mock `RateLimiter` 返回 `allowed=false`，断言 status=429、body 含业务码、Retry-After 存在**

使用 `MockServerWebExchange` + `GatewayFilterChain` 记录是否继续：

```java
AtomicBoolean continued = new AtomicBoolean();
GatewayFilterChain chain = ex -> { continued.set(true); return Mono.empty(); };
StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
assertFalse(continued.get());
assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
```

需 test 依赖：`spring-cloud-starter-gateway`（已 optional，test 可再声明非 optional）或 `spring-test` + gateway API。

- [ ] **Step 2–4: 红 → 实现 → 绿**

---

### Task 5: Servlet Filter（默认关闭）+ 模块全测

**Files:**
- Create: `servlet/RateLimitServletFilter.java`、`RateLimitServletAutoConfiguration.java`
- 条件：`@ConditionalOnWebApplication(SERVLET)` + `mdyaipay.ratelimit.servlet.enabled=true`（**matchIfMissing=false**）

逻辑镜像 Gateway（同步 `tryAcquire`），默认不装配即可；可只做编译期存在 + 一个 `@SpringBootTest` 在 `servlet.enabled=true` 时 Bean 存在的轻量测（可用 `spring-boot-starter-web` test 依赖，**optional/test**）。

若引入 web starter 过重：Servlet Filter **仅实现代码、不加强测**，在计划中注明 YAGNI 测留给 M4——优先保 Gateway 测。

**推荐：** Servlet 实现完整 + 无 WebTest；Gateway 测必须绿。

- [ ] **Step 1: 实现 Servlet Filter + AutoConfig**
- [ ] **Step 2: 全模块测试**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-ratelimit/mdyaipay-tools-ratelimit-spring-boot -am test -Dsurefire.failIfNoSpecifiedTests=false`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit（仅用户要求）**

```bash
git commit -m "$(cat <<'EOF'
feat(ratelimit): M3 Spring Boot 自动配置与 Gateway 429

EOF
)"
```

---

## Self-Review（对照 Spec M3）

| Spec 要求 | 任务 |
|-----------|------|
| `mdyaipay.ratelimit.*` 配置 | Task 2–3 |
| Gateway GlobalFilter + 429 + 头 | Task 4 |
| `RATE_LIMITED` / backend 不可用码 | Task 1 + 4 |
| fail-open / fail-closed | Task 4 |
| 可选 Servlet 默认关闭 | Task 5 |
| 不接 gateway 模块依赖 | 未列入（M4） |
| 不实现 Dubbo | 未列入 |

**占位符：** 无 TBD；body 内 appKey 明确不做。

**Order 说明：** `+5` 已写入 Global Constraints，避免被 collect 短路。

---

## Out of scope（M4 / P2）

- `mdyaipay-gateway` 引入依赖与 collect 默认 yaml
- `modules.md` 更新
- Trace `outcome=rate_limited`
- Dubbo Filter、动态规则、Testcontainers
