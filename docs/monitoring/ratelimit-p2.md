# 限流 P2（Servlet / SkyWalking / 压测报告）

在 M1 [`ratelimit-p1-prometheus.md`](ratelimit-p1-prometheus.md) 之上：

## Servlet

`mdyaipay.ratelimit.servlet.enabled=true` 时，`RateLimitServletFilter` 与 Gateway 共用 `RateLimitMetrics.record(...)`（Counter + WARN + SkyWalking Tag）。

## SkyWalking Tag（无硬依赖）

限流判定后（非 `skipped`），反射调用 `ActiveSpan.tag`：

| Tag | 示例 |
|-----|------|
| `ratelimit.rule_id` | `collect-post` |
| `ratelimit.outcome` | `denied` |

Gateway 另写入 Exchange 属性 `mdyaipay.ratelimit.ruleId` / `mdyaipay.ratelimit.outcome`，便于 Trace 模块读取。

需在进程挂载 Java Agent，且 classpath 含 `apm-toolkit-trace` 时 Tag 才会出现在 Span 上。

## 压测 HTTP 状态分布

`LoadTestReport.httpStatusCounts`：正式阶段按 `SampleOutcome.statusCode` 聚合，出现在 console / JSON / Markdown / HTML 报告。

限流验收示例（limit=20/s、高并发）：

- `200`（或业务 4xx）≈ 成功 TPS × 时长
- `429` 占错误主体
- `503` 应接近 0（除非 Redis/Redisson 故障且 `fail-open=false`）

**P3**（Alertmanager、SLO 告警、Grafana 模板）见 [`ratelimit-p3.md`](ratelimit-p3.md)。
