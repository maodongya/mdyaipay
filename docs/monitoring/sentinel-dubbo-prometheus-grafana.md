# Dubbo Sentinel：Dashboard + Prometheus + Grafana

## 1. Sentinel Dashboard（实时规则与 QPS）

业务 Pod 已通过 `mdyaipay.sentinel` 上报（`sentinel-transport-simple-http`）：

| 配置 | K8s 典型值 |
|------|------------|
| `MDYAIPAY_SENTINEL_DASHBOARD` | `sentinel-dashboard.mdyaipay.svc.cluster.local:8858` |
| `MDYAIPAY_SENTINEL_TRANSPORT_PORT` | 每实例不同（8719–8724） |
| 应用名 | `spring.application.name`（`mdyaipay-user` / `mdyaipay-payment` / `mdyaipay-gateway`） |

控制台：**http://127.0.0.1:8858**（默认 `sentinel` / `sentinel`）。

Dubbo 本机限流资源名（与代码一致）：

| 服务 | Provider 资源（Dashboard 流控规则） |
|------|-------------------------------------|
| user | `local:user-merchant-gateway-facade-provider` |
| payment | `local:payment-gateway-facade-provider` |
| gateway | Consumer：`local:gateway-user-merchant-gateway-facade-consumer`、`local:gateway-payment-facade-consumer` |

在对应应用下新增 **流控规则** 即可动态改 QPS；`cluster:` 前缀规则仍走 Redis 整体限流（见 [`../superpowers/specs/2026-09-21-sentinel-dual-ratelimit-design.md`](../superpowers/specs/2026-09-21-sentinel-dual-ratelimit-design.md)）。

## 2. Prometheus（本机 Sentinel 埋点）

`mdyaipay-tools-sentinel-spring-boot` 在 Dubbo / Gateway Filter 内计数：

| 指标 | 标签 |
|------|------|
| `mdyaipay_sentinel_entries_total` | `resource`、`outcome`（`passed` \| `blocked`）、`channel`（`dubbo_provider` \| `dubbo_consumer` \| `http`） |

抓取（K8s 已配置）：

```text
GET /actuator/prometheus   # user:8082 / payment:8081 / gateway:8041
```

PromQL 示例：

```promql
# Dubbo Provider 被拒绝 QPS（user / payment 看 job 切换）
sum(rate(mdyaipay_sentinel_entries_total{job="mdyaipay-user",outcome="blocked",channel="dubbo_provider"}[1m])) by (resource)

# 与 Dashboard 同名的 resource
mdyaipay_sentinel_entries_total{resource="local:payment-gateway-facade-provider"}
```

Recording Rules：`kubernetes/infra/monitoring/prometheus/rules/ratelimit-recording-rules.yaml` 中 `mdyaipay_sentinel_*`。

## 3. Grafana

看板：**mdyaipay 限流与服务流量**（uid `mdyaipay-ratelimit-gateway`）

- 本地 K8s：**http://127.0.0.1:3000/d/mdyaipay-ratelimit-gateway**
- 变量 **job**：选 `mdyaipay-user` 或 `mdyaipay-payment` 查看 **Sentinel Dubbo 放行/拒绝 QPS** 面板（resource 与 Dashboard 一致）

更新看板后：

```bash
kubectl apply -k kubernetes/infra/monitoring
curl -sf -X POST http://127.0.0.1:9090/-/reload
```

## 4. 与 Redis 整体限流的关系

| 层 | 观测 |
|----|------|
| Sentinel 本机 | `mdyaipay_sentinel_entries_total` + Dashboard 实时 QPS |
| Redis/Redisson 整体 | `mdyaipay_ratelimit_decisions_total` |

同一 Dubbo 请求先 Sentinel（-8600）再 Redis（-8500）；Grafana 上应同时看到两层指标（压测超过 limit 时）。
