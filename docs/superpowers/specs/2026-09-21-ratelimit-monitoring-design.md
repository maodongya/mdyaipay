# 限流与网关流量监控方案

> **范围：** 在现有 `mdyaipay-tools-ratelimit` + `mdyaipay-gateway` 上，可观测 **限流判定**（允许/拒绝/后端故障）与 **入口流量**（QPS、延迟、错误分布），支撑压测验收与生产告警。  
> **非目标（首版）：** 限流控制台、动态改阈值、按商户大屏（高基数维度仅日志采样）。

### M1 精简（当前实现）

| 保留 | 砍掉（M2+ 再做） |
|------|------------------|
| 单类 `RateLimitMetrics` + 1 Counter（`outcome` 标签区分 denied/allowed/…） | SPI `RateLimitObservation`、Context record、Outcome 枚举 |
| 可选 Timer `acquire` | 独立 `denied`/`fail_open`/`backend.errors` Counter |
| 拒绝 / 后端异常 WARN 日志 | 全量 access 日志、按商户 label |
| Gateway `actuator/prometheus` + 启动校验 | Grafana/Alertmanager 运维模板见 P3 |
| **P2** Servlet 埋点、SkyWalking Tag、loadtest status 分布 | 见 [`docs/monitoring/ratelimit-p2.md`](../../monitoring/ratelimit-p2.md) |

详见 [`docs/monitoring/ratelimit-p1-prometheus.md`](../../monitoring/ratelimit-p1-prometheus.md)。

---

## 1. 要回答的问题

| 角色 | 问题 |
|------|------|
| 运维 | 429 是否在涨？是限流生效还是 Redis/Redisson 挂了（503）？ |
| 开发 | 哪条 rule（collect / payout）被打满？fail-open 是否悄悄放行？ |
| 压测 | 正式阶段 **成功 TPS** 是否 ≈ 配置的 `limit/s`？429 占比是否合理？ |
| 业务 | 网关入口总流量、下游 Dubbo 是否跟得上（与 trace 联动）？ |

---

## 2. 监控分层

```text
                    ┌─────────────────────────────────────┐
  L3 告警/看板      │ Grafana / SkyWalking UI / 日志检索   │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
  L2 聚合指标       │ Prometheus（Micrometer）+ OAP 指标     │
                    └─────────────────┬───────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
  L1a 限流埋点                  L1b 网关 HTTP                 L1c 后端健康
  RateLimitGatewayFilter        SCG / Netty / Access Log      Redis TIME/Lua
  tryAcquire 结果               状态码、耗时、route            超时、NOSCRIPT
```

**原则（与 SOC / YAGNI 对齐）：**

- **埋点只在 `ratelimit-spring-boot`**（Filter / 可选 Servlet），不散落 gateway 业务 Filter。
- **指标维度低基数**：`rule_id`、`path`（配置里的 match path）、`algorithm`、`backend`、`outcome`；**禁止**把完整 `merchantAppKey`、订单号作为 Prometheus label。
- **高基数排查**走 **结构化日志 + Trace**（采样），不走时序库。

---

## 3. 核心指标（Micrometer → Prometheus）

命名前缀建议：`mdyaipay.ratelimit.*`（与配置前缀一致，便于关联）。

### 3.1 限流判定（必做 P1）

| 指标名 | 类型 | Tags | 含义 |
|--------|------|------|------|
| `mdyaipay.ratelimit.decisions.total` | Counter | `rule_id`, `outcome`, `backend` | 判定次数；`outcome=allowed\|denied\|backend_error\|skipped` |
| `mdyaipay.ratelimit.decisions.denied.total` | Counter | `rule_id` | 429 次数（= denied 子集，便于告警规则简单） |
| `mdyaipay.ratelimit.acquire.duration` | Timer | `rule_id`, `backend`, `outcome` | `tryAcquire` + Redis/Redisson 往返耗时 |
| `mdyaipay.ratelimit.backend.errors.total` | Counter | `backend`, `exception` | 后端异常；fail-open 时再打 `fail_open_applied=true` 日志 |

**`outcome=skipped` 触发条件（与现有 Filter 一致）：** 未启用、rules 为空、未匹配 rule、key 解析为空——便于发现「配置了但没生效」。

**从 `RateLimitDecision` 可选 Gauge（低频率更新）：**  
`mdyaipay.ratelimit.remaining`（`rule_id`）——仅在上报 allowed/denied 时用 **DistributionSummary** 记录 remaining 分布，**不要**按 logicalKey 建 label。

### 3.2 网关流量（必做 P1）

与限流互补，使用 Spring Cloud Gateway / Reactor 已有能力 + 少量定制：

| 指标名 | 类型 | Tags | 含义 |
|--------|------|------|------|
| `http.server.requests` | Timer | `method`, `uri`（模板化）, `status` | Boot 默认；**uri 须聚合为 route 模板**（见 §5.2） |
| `spring.cloud.gateway.requests` | Timer | `routeId`, `outcome`, `httpStatusCode` | SCG 指标（启用 `spring.cloud.gateway.metrics.enabled`） |
| `mdyaipay.gateway.requests.total` | Counter | `path_template`, `status` | 若默认 uri 基数过高时的兜底 Counter |

**关注状态码：** `200`、`400`、`429`、`503`（限流后端 fail-closed）、`502/504`（下游）。

### 3.3 Redis / Redisson 健康（P1 轻量 / P2 加强）

| 指标名 | 类型 | 说明 |
|--------|------|------|
| `mdyaipay.ratelimit.redis.time.duration` | Timer | `TIME` / 脚本执行耗时（Redisson `RScript`） |
| `mdyaipay.ratelimit.redis.script.reload.total` | Counter | NOSCRIPT 后 reload 次数，异常升高表示 Redis 被 flush 或实例漂移 |

**P2：** Redisson 连接池、Lettuce 连接事件（仅在 `backend=redis` 时）。

---

## 4. 日志与 Trace（P1 并行）

### 4.1 结构化日志（WARN 采样）

在 `RateLimitGatewayFilter` 拒绝或 backend 失败时（**不**对每个 allowed 打日志）：

```text
event=rate_limit_denied rule_id=gateway-payment-collect path=/api/v1/payments/collect
  limit=20 remaining=0 retry_after_ms=... backend=redisson
event=rate_limit_backend_error rule_id=... fail_open=false
```

- **采样：** 例如 denied 日志每 key 每 100 次 1 条，或固定 `logging.level` + 聚合指标为主。
- **禁止**在 info 打印完整 logicalKey（可 hash 后 8 位 troubleshooting）。

### 4.2 SkyWalking（已有 Agent 时）

| 能力 | 用法 |
|------|------|
| 入口 Span | 已有 HTTP；429 会体现为 **HTTP 状态 429** 的 Span |
| 自定义 Tag | P2：在 Filter 内 `Tags.STATUS_CODE` 已有；可加 `tag:ratelimit.rule_id`（需 Agent 支持自定义 Tag API） |
| 拓扑 | gateway → user/payment Dubbo 边；429 比例在 Endpoint 面板查看 |
| 告警 | OAP 规则：`endpoint_429_rate > X%` 持续 5m |

**与 TimeTrace：** 生产以 SkyWalking + Micrometer 为主；TimeTrace 不重复打限流。

---

## 5. 实现落点（代码，P1）

### 5.1 `ratelimit-spring-boot` 新增 SPI（推荐）

```java
/** 限流观测端口：core 不依赖 Micrometer。 */
public interface RateLimitObservation {
    void recordDecision(RateLimitObservationContext ctx);
}
```

- 默认实现：`MicrometerRateLimitObservation`（`optional` 依赖 `micrometer-core`）。
- 无 Micrometer 时：`NoOpRateLimitObservation`。
- **调用点唯一：** `RateLimitGatewayFilter`（及可选 `RateLimitServletFilter`）在 `tryAcquire` 前后。

`RateLimitObservationContext` 字段：`ruleId`, `path`, `method`, `outcome`, `limit`, `remaining`, `backend`, `durationNanos`, `errorClass`。

### 5.2 Gateway `application.yml`（示例）

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
  prometheus:
    metrics:
      export:
        enabled: true

spring:
  cloud:
    gateway:
      metrics:
        enabled: true
        tags:
          path:
            enabled: true   # 配合 route 定义，避免原始 URI 爆炸
```

**URI 模板化：** 依赖 SCG route（如 `/api/v1/payments/collect`）而非带 query 的完整 URI。

### 5.3 配置与指标联动

| 配置项 | 监控用途 |
|--------|----------|
| `mdyaipay.ratelimit.enabled` | 若为 false，不应有 `decisions` 增量（或全为 skipped） |
| `mdyaipay.ratelimit.backend` | label `backend=redisson\|redis\|memory` |
| `rules[].id` | label `rule_id` |
| `fail-open` | backend_error 时是否仍 `allowed`——用日志 + `fail_open_total` Counter |

---

## 6. 看板与告警（Grafana / Prometheus）

### 6.1 看板（建议 3 行）

1. **入口流量：** `sum(rate(http_server_requests_seconds_count{application="mdyaipay-gateway"}[1m])) by (uri, status)`  
2. **限流：** `sum(rate(mdyaipay_ratelimit_decisions_total{outcome="denied"}[1m])) by (rule_id)`  
3. **生效验证：** `sum(rate(...{outcome="allowed"}[1m])) by (rule_id)` — 压测 collect 时应 **≤ 配置的 limit**（滑动窗口允许瞬时误差）。

### 6.2 告警规则（示例）

| 告警 | PromQL 思路 | 说明 |
|------|-------------|------|
| 429 突增 | `rate(denied[5m]) / rate(decisions[5m]) > 0.5` 且 `rate(decisions[5m]) > 1` | 半数以上被拒且流量真实存在 |
| 限流后端故障 | `rate(backend_errors[1m]) > 0` 持续 2m | fail-closed → 503 |
| fail-open 风险 | 日志 `fail_open_applied` 或 Counter | 静默放行 |
| 限流「未生效」 | 高 QPS 但 `decisions{rule_id="gateway-payment-collect"}==0` | rule 未匹配或 Filter 未注册 |
| Redis 脚本异常 | `rate(redis_script_reload[5m]) > 0.1` | 需运维介入 |

---

## 7. 压测验收（与 loadtest 对齐）

对 `gateway-collect-10k.yaml` 类场景：

| 验收项 | 方法 |
|--------|------|
| 429 存在 | loadtest `errorSamples` 含 `status=429`；Prometheus `denied` > 0 |
| 成功 TPS ≈ limit | 正式阶段 `successCount / durationSeconds ≈ 20`（limit=20 时） |
| 无静默失效 | 启动日志 `gatewayFilter=registered`；`decisions.skipped` 不应等于总 HTTP 请求 |
| 报告归档 | 保留 `target/loadtest-reports/*.json` + 同期 Prometheus snapshot（或 Grafana 截图） |

**P2 已实现：** loadtest 报告 `httpStatusCounts`（200/429/503 等），由 `MetricsCollector` 聚合 `SampleOutcome.statusCode`。

---

## 8. 分阶段交付

| 阶段 | 内容 | 依赖 |
|------|------|------|
| **M1** | `RateLimitMetrics` + 单 Counter；Gateway prometheus；WARN 日志 | **已实现（精简）** |
| **M2+** | 本文 §3 完整指标族、SPI、SkyWalking、Servlet、告警模板 | 待做 |
| **P2** | Servlet 限流同样埋点；SkyWalking 自定义 Tag；loadtest 报告 status 分布 | **已实现** |
| **P3** | 按环境推送告警（Alertmanager）；SLO 错误预算（429 作为「预期拒绝」与 5xx 分离） | **已实现（运维模板）** — [`docs/monitoring/ratelimit-p3.md`](../../monitoring/ratelimit-p3.md) |

---

## 9. 与现有文档关系

- 限流行为与配置：[`2026-09-20-ratelimit-design.md`](2026-09-20-ratelimit-design.md)  
- 链路 APM：[`../../skywalking-integration.md`](../../skywalking-integration.md)  
- 压测：[`../../loadtest-design.md`](../../loadtest-design.md)  

**下一步实现：** 在 `mdyaipay-tools-ratelimit-spring-boot` 增加 P1 埋点 + gateway 打开 `prometheus` 端点；不在此 spec 内改业务模块。
