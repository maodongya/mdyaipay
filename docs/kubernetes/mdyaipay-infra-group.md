# mdyaipay 基础设施组（Docker Desktop Kubernetes）

命名空间 **`mdyaipay-infra`** 统一部署本地依赖，标签 **`app.kubernetes.io/part-of: mdyaipay`**、**`app.kubernetes.io/component: infra`**。

| 组件 | StatefulSet / Service | 宿主机端口 | 文档 |
|------|-------------------------|------------|------|
| MySQL 8 | `mysql8` | 3306 | [mysql8-docker-desktop.md](mysql8-docker-desktop.md) |
| Redis 7 | `redis7` | 6379 | [redis7-docker-desktop.md](redis7-docker-desktop.md) |
| ZooKeeper 3 节点 | `zookeeper` ×3 | 2181 | [zookeeper-docker-desktop.md](zookeeper-docker-desktop.md) |
| Prometheus | `prometheus` | 9090 | [monitoring-docker-desktop.md](monitoring-docker-desktop.md) |
| Grafana | `grafana` | 3000 | [monitoring-docker-desktop.md](monitoring-docker-desktop.md) |

## 全量重建（命名空间丢失 / PVC 清空）

优先从 `.local/*-migrate/` 备份恢复 MySQL / Redis / ZK：

```bash
./scripts/rebuild-mdyaipay-infra-k8s.sh
```

MySQL root Secret 仍需脚本生成：

```bash
# MySQL Secret + 全组（MySQL / Redis / ZK）
./scripts/apply-mdyaipay-mysql-k8s.sh   # 若尚未部署 MySQL
kubectl apply -k kubernetes/infra        # 会 reconcile 已存在的 mysql/redis + zookeeper
```

或分别：

```bash
./scripts/apply-mdyaipay-mysql-k8s.sh
./scripts/apply-mdyaipay-redis-k8s.sh
./scripts/apply-mdyaipay-zookeeper-k8s.sh
./scripts/apply-mdyaipay-monitoring-k8s.sh
```

## Docker Compose 业务栈连接

| 变量 | 典型值 |
|------|--------|
| JDBC | `host.docker.internal:3306` |
| `MDYAIPAY_RATELIMIT_REDIS_URI` | `redis://host.docker.internal:6379` |
| `DUBBO_REGISTRY_ADDRESS` | `zookeeper://host.docker.internal:2181` |

```bash
./scripts/run-mdyaipay-docker.sh
```

业务栈迁 K8s 见 [mdyaipay-apps-docker-desktop.md](mdyaipay-apps-docker-desktop.md)：

```bash
./scripts/migrate-mdyaipay-docker-to-k8s.sh
```

## 目录

```text
kubernetes/infra/kustomization.yaml    # 聚合 mysql + redis + zookeeper + monitoring
kubernetes/infra/monitoring/           # Prometheus + Grafana
kubernetes/infra/zookeeper/
kubernetes/mysql/
kubernetes/redis/
```
