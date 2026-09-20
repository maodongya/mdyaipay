# Redis 7 迁移至 Docker Desktop Kubernetes 设计

> **环境：** Docker Desktop Kubernetes，命名空间 **`mdyaipay-infra`**（与 MySQL 8 同）。  
> **数据：** Docker `redis7` 的 RDB（`dump.rdb`）复制到 StatefulSet PVC。  
> **范围：** 限流 Redisson/Lettuce 使用的 Redis；本地默认无密码。

---

## 1. 目标

- StatefulSet **`redis7`**，镜像 **`redis:7.2`**，PVC **hostpath** 5Gi。
- Service **LoadBalancer :6379** → `127.0.0.1:6379` / `host.docker.internal:6379`。
- 脚本：`apply-mdyaipay-redis-k8s.sh`、`migrate-redis7-docker-to-k8s.sh`。
- `run-mdyaipay-docker.sh` 依赖检查包含 K8s Redis Ready。

## 2. 非目标

- Redis Cluster / Sentinel。
- 启用 AUTH（本地与现 `redis7` 一致，无密码）。

## 3. 迁移顺序

1. Docker `SAVE` / `redis-cli --rdb` 导出 `dump.rdb`。
2. 停止 Docker `redis7`，释放 6379。
3. Apply K8s；Scale 0 → 辅助 Pod 挂载 `data-redis7-0` 写入 RDB → Scale 1。
4. `redis-cli PING` 校验。

## 4. 连接

| 场景 | URI |
|------|-----|
| Docker Compose | `redis://host.docker.internal:6379`（已与 compose 默认一致） |
| 集群内 Pod | `redis://redis7.mdyaipay-infra.svc.cluster.local:6379` |
