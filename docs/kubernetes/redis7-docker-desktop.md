# Redis 7 on Docker Desktop Kubernetes

**mdyaipay-infra** 命名空间内的单实例 Redis 7.2，替代本机 Docker 容器 `redis7`。与 [MySQL 8](mysql8-docker-desktop.md) 同集群。

## 前置

- Docker Desktop Kubernetes 已启用（`docker-desktop`）。
- 默认 StorageClass **hostpath**。
- 本地无 Redis AUTH（与现 `redis7` 一致）。

## 首次从 Docker 迁移（含 RDB）

```bash
chmod +x scripts/apply-mdyaipay-redis-k8s.sh scripts/migrate-redis7-docker-to-k8s.sh
./scripts/migrate-redis7-docker-to-k8s.sh
```

流程：`SAVE` → 停止 Docker `redis7` → 创建 PVC → 写入 `dump.rdb` → 启动 StatefulSet。

备份目录：`.local/redis7-migrate/`。

## 仅部署空实例

```bash
./scripts/apply-mdyaipay-redis-k8s.sh
```

## 连接方式

| 场景 | URI |
|------|-----|
| Docker Compose（限流 Redisson） | `redis://host.docker.internal:6379` |
| 宿主机 | `redis://127.0.0.1:6379` |
| 同集群 Pod | `redis://redis7.mdyaipay-infra.svc.cluster.local:6379` |

环境变量：`MDYAIPAY_RATELIMIT_REDIS_URI`（见 `docker/services/docker-compose.yml`）。

## 清单

| 文件 | 说明 |
|------|------|
| `kubernetes/redis/statefulset.yaml` | `redis:7.2`，AOF，5Gi PVC |
| `kubernetes/redis/service.yaml` | headless + LoadBalancer :6379 |

## 运维

```bash
kubectl get pod,svc,pvc -n mdyaipay-infra -l app.kubernetes.io/name=redis7
kubectl logs -n mdyaipay-infra redis7-0 -c redis -f
redis-cli -h 127.0.0.1 -p 6379 ping
```

删除 PVC 会丢数据；删前请保留 RDB。

## 回滚到 Docker redis7

1. `kubectl scale statefulset redis7 -n mdyaipay-infra --replicas=0`
2. `docker start redis7`
3. 必要时将 `.local/redis7-migrate/*.rdb` 拷回 Docker 数据目录。

设计说明：`docs/superpowers/specs/2026-09-21-redis7-k8s-design.md`
