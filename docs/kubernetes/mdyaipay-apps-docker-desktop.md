# mdyaipay 业务栈（Docker Desktop Kubernetes）

命名空间 **`mdyaipay`**：Sentinel Dashboard + user / payment / gateway **各 2 节点**。  
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
| Sentinel | http://127.0.0.1:8858 |

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

## 停止

```bash
./scripts/stop-mdyaipay-k8s.sh
```

## 清单

```text
kubernetes/apps/
  namespace.yaml
  sentinel.yaml
  user.yaml
  payment.yaml
  gateway.yaml
  kustomization.yaml
```

Prometheus 抓取已改为 `*.mdyaipay.svc.cluster.local`（见 `kubernetes/infra/monitoring/prometheus/prometheus.yml`）。
