# 限流 M1 监控（精简）

Gateway 启动后：

```text
GET http://127.0.0.1:8041/actuator/prometheus
```

## M1 只认两个产物

| 产物 | 作用 |
|------|------|
| **1 个 Counter** | `mdyaipay_ratelimit_decisions_total{rule_id,outcome,backend}` |
| **可选 Timer** | `mdyaipay_ratelimit_acquire_seconds`（tryAcquire 耗时） |
| **WARN 日志** | `event=rate_limit_denied` / `event=rate_limit_backend_error` |

`outcome`：`allowed` | `denied` | `backend_error` | `skipped`。

## PromQL（够用即可）

```promql
# 拒绝 QPS
sum(rate(mdyaipay_ratelimit_decisions_total{outcome="denied"}[1m])) by (rule_id)

# 放行 QPS（应 ≈ YAML limit/s）
sum(rate(mdyaipay_ratelimit_decisions_total{outcome="allowed"}[1m])) by (rule_id)
```

## 代码入口

单类 `RateLimitMetrics`（无 SPI、无 Context record）。Filter 内调用 `metrics.record(...)`。

**P2**（Servlet 埋点、SkyWalking Tag、压测 status 分布）见 [`ratelimit-p2.md`](ratelimit-p2.md)。  
**P3**（Alertmanager 路由、SLO Recording/告警、Grafana 模板）见 [`ratelimit-p3.md`](ratelimit-p3.md)。  
fail_open 独立 Counter 仍留 **M2+**，见 [`../superpowers/specs/2026-09-21-ratelimit-monitoring-design.md`](../superpowers/specs/2026-09-21-ratelimit-monitoring-design.md)。
