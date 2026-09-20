# Sentinel 单机 + Redis 整体双限流

## 分层

| 层 | 实现 | 粒度 |
|----|------|------|
| 本机 | Sentinel `FlowRule`，资源 `local:{ruleId}` | 每个 JVM 独立 QPS |
| 整体 | 既有 `mdyaipay.ratelimit` + Redisson/Redis | 全集群共享 limit |

请求顺序：**Sentinel 本机（Gateway order +4）→ Redis 整体（+5）→ 业务**。

## Dashboard 配置约定

| 资源名前缀 | 作用 |
|------------|------|
| `local:gateway-payment-collect` | 仅本机 Sentinel 限流 |
| `cluster:gateway-payment-collect` | 同步改 yaml 同 id 的 **整体** `policy.limit`（Redis 执行），不在本机 Sentinel 扣减 |

应用在控制台选择 **spring.application.name**（如 `mdyaipay-gateway`）后增删流控规则即可。

## Docker（compose 组 `mdyaipay-infra`）

```bash
./scripts/run-sentinel-dashboard-docker.sh
```

| 服务 | 容器名 | 地址 |
|------|--------|------|
| Sentinel Dashboard | `mdyaipay-sentinel-dashboard` | http://127.0.0.1:8858（sentinel / sentinel） |
| Nacos standalone | `mdyaipay-nacos` | http://127.0.0.1:8848/nacos |

业务 compose 中 gateway/user/payment 各实例 `MDYAIPAY_SENTINEL_TRANSPORT_PORT` 互不冲突（8719–8724）。Nacos 与 Sentinel 同文件：`docker/sentinel/docker-compose.yml`。

整体限流规则接入 Nacos 见 [`docs/nacos/ratelimit-nacos.md`](../../nacos/ratelimit-nacos.md)。
