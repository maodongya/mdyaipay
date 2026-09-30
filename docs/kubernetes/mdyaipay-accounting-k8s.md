# mdyaipay-accounting（Kubernetes）

命名空间 **`mdyaipay`**，与 user / payment 相同模式：**2 个 Deployment + LoadBalancer Service**。

## 清单

- [`kubernetes/apps/accounting.yaml`](../../kubernetes/apps/accounting.yaml)（已纳入 `kubernetes/apps/kustomization.yaml`）

## 端口（Docker Desktop LoadBalancer → localhost）

| 实例 | HTTP | Dubbo（LB） | 容器 HTTP | 容器 Dubbo |
|------|------|-------------|-----------|------------|
| accounting-1 | 8085 | 20885 | 8083 | 20883 |
| accounting-2 | 8086 | 20886 | 8083 | 20883 |

> user-2 已占用宿主机 **8083**，故账务对外 HTTP 使用 **8085 / 8086**。

## 依赖（集群内 DNS）

| 变量 | 值 |
|------|-----|
| `ACCOUNTING_JDBC_URL` | `mysql8.mdyaipay-infra.svc.cluster.local:3306/mdyaipay_accounting` |
| `DUBBO_REGISTRY_ADDRESS` | `zookeeper://zookeeper.mdyaipay-infra.svc.cluster.local:2181` |
| `MDYAIPAY_SENTINEL_DASHBOARD` | `sentinel-dashboard.mdyaipay.svc.cluster.local:8858` |

Secret **`mdyaipay-jdbc`** 键 **`ACCOUNTING_JDBC_PASSWORD`**（与 MySQL root 一致，由 `run-mdyaipay-k8s.sh` 创建）。

## 收单入账（Dubbo）

payment 默认 **`PAYMENT_ACCOUNTING_CHANNEL=dubbo`**：收单 `SUCCESS` 后调用账务 **`CollectAccountingFacade`**（Triple / `tri`，ZK 发现 `mdyaipay-accounting` Pod）。

- 幂等键：`collect:{orderNo}` → 商户 **待结算** 增加
- 无 `merchantId` 的订单：账务 Facade no-op，仍返回 success

可选 **`PAYMENT_ACCOUNTING_CHANNEL=mq`**（需集群部署 RocketMQ 并去掉 payment/accounting 的 RocketMQ `SPRING_AUTOCONFIGURE_EXCLUDE`）。

账务侧 MQ 消费者仅在 `ACCOUNTING_MQ_ENABLED=true` 时装配；K8s 默认关闭。

## 部署

随全栈脚本（会构建 `mdyaipay-accounting:local` 镜像）：

```bash
./scripts/run-mdyaipay-k8s.sh
```

仅重建 user / payment / accounting：

```bash
./scripts/redeploy-mdyaipay-user-payment-k8s.sh
```

健康检查：

```bash
curl -sf http://127.0.0.1:8085/actuator/health
curl -sf http://127.0.0.1:8086/actuator/health
```

Prometheus job **`mdyaipay-accounting`** 抓取 `:8083/actuator/prometheus`（集群内 Service DNS）。
