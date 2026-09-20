# RateLimit M4（gateway 接入 + collect 令牌桶 + 文档）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `mdyaipay-gateway` 引入 `mdyaipay-tools-ratelimit-spring-boot`（及 Redis 驱动），对 `POST /api/v1/payments/collect` 默认启用 **令牌桶 300/s**；补齐 `modules.md` / README 说明。算法与 Filter 仍在 tools，gateway 只装配配置。

**Architecture:** gateway 依赖 spring-boot 自动配置 → 本地默认 `backend=redis` + `redis://127.0.0.1:6379`（对齐已有 `redis7`）；无 Redis 时运维可改 `memory` 或 `fail-open`。RateLimit Filter（Order +5）在 collect 验签 Filter（+10）之前。

**Tech Stack:** 已有 M1–M3；Spring Cloud Gateway；本机 redis7。

**Spec:** `docs/superpowers/specs/2026-09-20-ratelimit-design.md` §7.1 示例、§11 M4、§13 待确认项（本计划拍板）。

**前置：** M3 源码须在工作区（当前多为未提交文件）；执行 M4 前可先提交 M3，或与 M4 同批提交。

## Global Constraints

- gateway **只**依赖 `mdyaipay-tools-ratelimit-spring-boot` + 运行期需要的 `mdyaipay-tools-ratelimit-redis`（因 spring-boot 对 redis 为 optional，须显式引入才能用 Redis 后端）
- **不**在 gateway 内写算法 / Lua / INCR
- collect 默认：`TOKEN_BUCKET`，`limit=300`，`refill-rate-per-second=300`，`window=1s`（对齐压测 ~300 RPS）
- `key-resolvers: [merchantAppKey, clientIp]`：body 内 appKey **不可用**（限流在验签前）；无 `X-App-Key`/query 时键退化为 `clientIp`（composer 跳过 null）——文档写明
- Redis URI 默认 `redis://127.0.0.1:6379`；`fail-open: false`（与 Spec 生产默认一致）；本地无 Redis 可设 `MDYAIPAY_RATELIMIT_FAIL_OPEN=true` 或 `backend=memory`
- `modules.md` 已有 ratelimit 行：本里程碑 **补充** gateway 依赖说明与限流配置入口，避免重复堆砌
- 中文注释/文档；提交仅用户要求时

### 拍板（Spec §13）

1. Redis 本地默认 → **`redis7:6379`**（`redis://127.0.0.1:6379`）
2. collect 默认频率 → **300/s 令牌桶**
3. M1–M3 已完成；本计划为 M4

---

## File Structure

```
mdyaipay-gateway/pom.xml                         + ratelimit-spring-boot + ratelimit-redis
mdyaipay-gateway/src/main/resources/application.yml   + mdyaipay.ratelimit.*
docs/modules.md                                  补充 gateway↔ratelimit 依赖一句
README.md                                        限流配置短节（可选，与 modules 不重复则极短）
docs/superpowers/specs/2026-09-20-ratelimit-design.md  §13 标为已确认（可选小改）
mdyaipay-gateway/.../RateLimitGatewayWiringTest.java  上下文冒烟（可选）
```

---

### Task 1: gateway Maven 依赖

**Files:**
- Modify: `mdyaipay-gateway/pom.xml`

**Produces:** 编译期可见 `RateLimitAutoConfiguration` / `RedisRateLimiter`

```xml
<dependency>
    <groupId>com.mdyaipay</groupId>
    <artifactId>mdyaipay-tools-ratelimit-spring-boot</artifactId>
</dependency>
<dependency>
    <groupId>com.mdyaipay</groupId>
    <artifactId>mdyaipay-tools-ratelimit-redis</artifactId>
</dependency>
```

根 `dependencyManagement` 已锁定两 artifact（先前骨架已加）。若缺失则补。

- [ ] **Step 1: 加入依赖**
- [ ] **Step 2: 编译 gateway**

Run: `mvn -pl mdyaipay-gateway -am compile -q`

Expected: SUCCESS（需 M3 类已在 ratelimit-spring-boot）

- [ ] **Step 3: Commit（仅用户要求；可与配置同 commit）**

---

### Task 2: application.yml 默认 collect 令牌桶

**Files:**
- Modify: `mdyaipay-gateway/src/main/resources/application.yml`

在现有 `mdyaipay:` 下增加（可用环境变量覆盖）：

```yaml
  ratelimit:
    enabled: ${MDYAIPAY_RATELIMIT_ENABLED:true}
    backend: ${MDYAIPAY_RATELIMIT_BACKEND:redis}
    fail-open: ${MDYAIPAY_RATELIMIT_FAIL_OPEN:false}
    redis:
      uri: ${MDYAIPAY_RATELIMIT_REDIS_URI:redis://127.0.0.1:6379}
      timeout: 200ms
    default-policy:
      algorithm: TOKEN_BUCKET
      limit: 500
      window: 1s
      refill-rate-per-second: 500
    rules:
      - id: gateway-collect
        match:
          path: /api/v1/payments/collect
          methods: [POST]
        policy:
          algorithm: TOKEN_BUCKET
          limit: 300
          window: 1s
          refill-rate-per-second: 300
        key-resolvers: [merchantAppKey, clientIp]
```

与 `profiles/gateway-collect-token-bucket.yaml` 对齐；运行时以 application.yml 为准。

- [ ] **Step 1: 写入 yml**
- [ ] **Step 2: 启动冒烟（可选）** — 若本机 redis7 + gateway 可起：`curl -X POST` 超限应 429；无环境则跳过，依赖 Task 3 文档与编译

---

### Task 3: 文档（modules.md + README + Spec §13）

**Files:**
- Modify: `docs/modules.md` — 在「依赖关系」补一句：`gateway` 引入 `mdyaipay-tools-ratelimit-spring-boot`（Redis Lua 经 `ratelimit-redis`）；collect 默认令牌桶见 gateway `application.yml`
- Modify: `README.md` — 网关小节增加 3～5 行：限流开关/后端/Redis URI 环境变量；链到 design spec
- Modify: `docs/superpowers/specs/2026-09-20-ratelimit-design.md` §13 — 三项标为已确认（Redis=6379、collect=300/s、M1–M4 已实施路径）

- [ ] **Step 1: 改 modules.md**
- [ ] **Step 2: 改 README（保持简短）**
- [ ] **Step 3: 更新 Spec §13**

---

### Task 4: 轻量验证

**Files:**
- Optional Create: `mdyaipay-gateway/src/test/java/.../RateLimitGatewayContextIT.java`

若 gateway 已有 SpringBootTest 模式，复用；否则 **不做全量启动测**（依赖多：zk/dubbo），仅：

```bash
mvn -pl mdyaipay-gateway -am test -Dsurefire.failIfNoSpecifiedTests=false
```

确保既有 gateway 单测不因新依赖红；ratelimit-spring-boot 单测仍绿。

- [ ] **Step 1: 跑 gateway + ratelimit 相关模块测试**
- [ ] **Step 2: Commit（仅用户要求）**

```bash
git commit -m "$(cat <<'EOF'
feat(gateway): M4 接入限流并默认 collect 令牌桶

EOF
)"
```

---

## Self-Review（对照 Spec M4）

| Spec 要求 | 任务 |
|-----------|------|
| gateway 引入 spring-boot 依赖 | Task 1 |
| collect 默认令牌桶 | Task 2 |
| 文档与 modules.md | Task 3 |
| 不把算法写进 gateway | 遵守 |
| Redis 本地 6379 / 300 RPS | Global Constraints 已拍板 |

**占位符：** 无 TBD。

**已知限制：** collect 键在无 `X-App-Key` 时仅为 clientIp；商户维度需压测/客户端带头，或后续在验签后二次限流（P2，YAGNI）。

---

## Out of scope

- Trace `outcome=rate_limited`
- loadtest 429 断言场景（Spec P2）
- payment/user Servlet 限流开启
- Dubbo Filter
