# 限流 P3（Alertmanager / SLO / 看板模板）

在 M1 [`ratelimit-p1-prometheus.md`](ratelimit-p1-prometheus.md) 与 P2 [`ratelimit-p2.md`](ratelimit-p2.md) 之上，向 **运维平台** 交付可挂载的 Prometheus / Alertmanager 片段。应用代码 **无需改动**。

## 产物清单

| 文件 | 用途 |
|------|------|
| [`prometheus/ratelimit-recording-rules.yaml`](prometheus/ratelimit-recording-rules.yaml) | SLO 用 Recording Rules（429 与 5xx 分离） |
| [`prometheus/ratelimit-alerts.yaml`](prometheus/ratelimit-alerts.yaml) | 限流与网关告警规则 |
| [`alertmanager/ratelimit-routes.example.yaml`](alertmanager/ratelimit-routes.example.yaml) | 按 `env` 分流的接收器示例 |
| [`grafana/ratelimit-gateway-dashboard.json`](grafana/ratelimit-gateway-dashboard.json) | 三行看板（入口 / 拒绝 / 放行） |

挂载方式（示意）：

```yaml
# prometheus.yml 片段
rule_files:
  - /etc/prometheus/rules/ratelimit-recording-rules.yaml
  - /etc/prometheus/rules/ratelimit-alerts.yaml

scrape_configs:
  - job_name: mdyaipay-gateway
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['gateway:8041']
        labels:
          env: prod
          application: mdyaipay-gateway
```

校验：

```bash
promtool check rules docs/monitoring/prometheus/ratelimit-recording-rules.yaml
promtool check rules docs/monitoring/prometheus/ratelimit-alerts.yaml
```

## SLO：429 不算「可用性失败」

网关 **预期** 在超限时返回 **429**；与 **503**（限流后端 fail-closed）、**502/504**（下游）分开统计。

| 口径 | Recording / PromQL 思路 |
|------|-------------------------|
| **错误预算（扣减）** | 仅 `status=~"5.."` 的 HTTP 请求速率 |
| **成功 + 预期拒绝** | `status=~"2..|429"` |
| **可用性比率（30d 滚动示例）** | `1 - (5xx_rate / total_rate)`，其中 total 含 429 |

Recording 名前缀 `mdyaipay:gateway:*` 见 YAML；告警 **RateLimitHigh5xxRate** 与 **RateLimit429Spike** 分开，避免把压测时的 429 当成故障页。

## 告警与 P1 指标对应

| 告警名 | 依赖指标 | 说明 |
|--------|----------|------|
| `RateLimit429Spike` | `mdyaipay_ratelimit_decisions_total{outcome="denied"}` | 拒绝占比过高且确有流量（非静默） |
| `RateLimitBackendErrors` | `outcome="backend_error"` | Redis/Redisson 异常；fail-closed 时伴随 503 |
| `RateLimitDecisionsAbsent` | HTTP QPS 高但 `decisions` 为 0 | 规则未匹配或 Filter 未注册 |
| `RateLimitHigh5xxRate` | `http_server_requests` 5xx | SLO 错误预算；**不含** 429 |
| `GatewayHighLatency` | `http_server_requests` P99 | 可选容量信号 |

**fail-open：** M1 无独立 Counter，仍以 WARN `event=rate_limit_backend_error fail_open=true` + `backend_error` 指标为主；M2+ 可增 `fail_open_total` 后收紧告警。

## Alertmanager 按环境推送

示例路由见 [`alertmanager/ratelimit-routes.example.yaml`](alertmanager/ratelimit-routes.example.yaml)：

- **`env=prod`** + `severity=critical` → 值班/on-call
- **`env=staging`** → 团队 Slack/飞书
- **`env=dev`** → 静默或仅 Webhook 日志

Prometheus `external_labels.env` 与 scrape `labels.env` 须与 Alertmanager `match` 一致。

## Grafana

导入 [`grafana/ratelimit-gateway-dashboard.json`](grafana/ratelimit-gateway-dashboard.json)。看板 v2 含 **采集 up**、**429 独立曲线**、**全 outcome** 面板。

本地 pay-settle 监控栈一键同步：

```bash
chmod +x scripts/sync-local-monitoring.sh
./scripts/sync-local-monitoring.sh
```

## 看图诊断（是否符合预期）

| 现象 | 含义 | 处理 |
|------|------|------|
| **denied / allowed = No data**，入口仅 **503** 小尖刺 | Prometheus `job=mdyaipay-gateway` **down**，或压测未打到 collect | 确认 `8041` 网关进程、`curl :8041/actuator/prometheus` 含 `mdyaipay_ratelimit_decisions_total`；Targets 页应为 **UP** |
| 压测中 **allowed ≈ 20/s**，**denied** 上升，**429** 曲线有量 | 限流生效，符合 M1/P2 验收 | 正常 |
| **503** 持续（非尖刺）且 **backend_error** 涨 | Redisson/Redis 故障 + fail-closed | 查 Redis、网关 WARN `rate_limit_backend_error` |
| **5xx 占比** 面板有值但 **429** 很高 | 若 5xx 来自 503 而非 429，仍算 SLO 问题；429 单独看下一 panel | 429 不计入 5xx SLO |
| **skipped** 占比高 | 规则未匹配或 key 为空 | 查 YAML rules、Filter 注册日志 |

**本次截图结论：** 2h 窗口内几乎无有效业务流量；01:17 附近 **503** 尖刺更像 **网关短暂不可用或 fail-closed**，不是「限流压测通过」形态。**不符合**压测验收预期，优先修 **采集 UP + 网关常驻** 再跑 5min loadtest。

## 压测验收（与 P2 报告联动）

正式阶段同时满足：

1. loadtest `httpStatusCounts` 中 **429** 占错误主体，**503** ≈ 0  
2. Prometheus `sum(rate(mdyaipay_ratelimit_decisions_total{outcome="allowed"}[1m])) by (rule_id)` ≈ YAML `limit/s`  
3. **不**触发 `RateLimitDecisionsAbsent`；可预期触发或接近 `RateLimit429Spike` 阈值（调 staging 阈值或 `inhibit` 压测窗口）

归档：`target/loadtest-reports/*.json` + 同期 Grafana 截图或 Prometheus snapshot。

## 设计溯源

[`../superpowers/specs/2026-09-21-ratelimit-monitoring-design.md`](../superpowers/specs/2026-09-21-ratelimit-monitoring-design.md) §6、§8 P3。
