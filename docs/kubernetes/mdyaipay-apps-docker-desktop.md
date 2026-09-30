# mdyaipay 业务栈（Docker Desktop Kubernetes）

命名空间 **`mdyaipay`**：Sentinel + **finance-mock / finance-gateway / finance** + user / payment / accounting / gateway **各 2 节点**（账务 [mdyaipay-accounting-k8s.md](mdyaipay-accounting-k8s.md)，渠道 [mdyaipay-finance-channel-k8s.md](mdyaipay-finance-channel-k8s.md)）。  
基础设施在 **`mdyaipay-infra`**（见 [mdyaipay-infra-group.md](mdyaipay-infra-group.md)）。

## 部署

```bash
# 需已部署 mdyaipay-infra
./scripts/rebuild-mdyaipay-infra-k8s.sh   # 若 infra 缺失

# 从 Docker 迁移（停 compose 业务容器 + 构建镜像 + apply）
./scripts/migrate-mdyaipay-docker-to-k8s.sh

# 或仅 K8s（会 mvn package + docker build）
./scripts/run-mdyaipay-k8s.sh
MDYAIPAY_SKIP_BUILD=true ./scripts/run-mdyaipay-k8s.sh   # 跳过构建
```

## 访问（LoadBalancer → localhost）

| 服务 | URL |
|------|-----|
| Gateway | http://127.0.0.1:8041 、8042 |
| User | http://127.0.0.1:8082 、8083 |
| Payment | http://127.0.0.1:8081 、8084 |
| Accounting | http://127.0.0.1:8085 、8086 |
| Finance mock | http://127.0.0.1:8097 、8098 |
| Finance gateway | http://127.0.0.1:8087 、8088 |
| Finance（对账） | http://127.0.0.1:8091 、8092 |
| Sentinel | http://127.0.0.1:8858 |

联调冒烟：`./scripts/run-mdyaipay-k8s-smoke.sh`（依赖上表 gateway/user/payment 已就绪）。

## 集群内 DNS

| 依赖 | 地址 |
|------|------|
| MySQL | `mysql8.mdyaipay-infra.svc.cluster.local:3306` |
| Redis | `redis7.mdyaipay-infra.svc.cluster.local:6379` |
| ZooKeeper | `zookeeper.mdyaipay-infra.svc.cluster.local:2181` |
| Sentinel | `sentinel-dashboard.mdyaipay.svc.cluster.local:8858` |

Dubbo 注册 IP 为 **Pod IP**（`DUBBO_IP_TO_REGISTRY`）。

## 镜像

本地构建 tag：`:local`，`imagePullPolicy: IfNotPresent`（Docker Desktop 共享 daemon）。

## 资源（Docker Desktop）

全栈 **双节点** 约需 **8GiB+** 分配给 Kubernetes（Settings → Resources）。若 Pod 长期 `Pending` 且 Events 为 `Insufficient memory`，可先 `./scripts/stop-mdyaipay-k8s.sh` 再提高内存后 `./scripts/run-mdyaipay-k8s.sh`，或临时只保留各服务 `-1` 节点做联调。

## 停止

```bash
./scripts/stop-mdyaipay-k8s.sh
```

## 清单

```text
kubernetes/apps/
  namespace.yaml
  sentinel.yaml
  finance-mock.yaml
  finance-gateway.yaml
  finance.yaml
  user.yaml
  payment.yaml
  accounting.yaml
  gateway.yaml
  kustomization.yaml
```

Prometheus 抓取已改为 `*.mdyaipay.svc.cluster.local`（见 `kubernetes/infra/monitoring/prometheus/prometheus.yml`）。
