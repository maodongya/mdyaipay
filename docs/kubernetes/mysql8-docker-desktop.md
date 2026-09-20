# MySQL 8 on Docker Desktop Kubernetes

本目录为 **mdyaipay-infra** 命名空间内的单实例 MySQL 8，替代本机 Docker 容器 `mysql8`。

基础设施组总览：[mdyaipay-infra-group.md](mdyaipay-infra-group.md)。

## 前置

1. Docker Desktop → **Kubernetes** 已启用，context 为 `docker-desktop`。
2. `kubectl get sc` 存在默认 **hostpath** StorageClass。
3. 迁移前 Docker `mysql8` 密码与 `MDYAIPAY_MYSQL_ROOT_PASSWORD` 一致（本地默认 `123456`）。

## 首次从 Docker 迁移（含数据）

```bash
chmod +x scripts/apply-mdyaipay-mysql-k8s.sh scripts/migrate-mysql8-docker-to-k8s.sh
./scripts/migrate-mysql8-docker-to-k8s.sh
```

脚本会：mysqldump → 停止 Docker `mysql8` → apply 清单 → 导入 SQL。

备份默认目录：`.local/mysql8-migrate/`（已 gitignore 建议在本地使用）。

## 仅部署空实例（无 Docker 数据）

```bash
./scripts/apply-mdyaipay-mysql-k8s.sh
```

业务库可由应用 `init-schema` + JDBC `createDatabaseIfNotExist=true` 创建。

## 连接方式

| 场景 | JDBC / 主机 |
|------|-------------|
| Docker Compose 业务容器 | `host.docker.internal:3306` |
| 宿主机 mysql 客户端 | `127.0.0.1:3306` |
| 未来 Pod 在同集群 | `mysql8.mdyaipay-infra.svc.cluster.local:3306` |

Service 类型为 **LoadBalancer**（Docker Desktop 映射到 `localhost:3306`）。

## 清单文件

| 文件 | 说明 |
|------|------|
| `namespace.yaml` | `mdyaipay-infra` |
| `configmap-mysqld.yaml` | mysqld 调优（与原 `99-mdyaipay.cnf` 一致） |
| `statefulset.yaml` | `mysql:8.0`，2Gi 内存，20Gi PVC |
| `service.yaml` | headless + LoadBalancer :3306 |
| `secret-root.yaml.example` | 示例；真实 Secret 由 apply 脚本创建 |

## 运维

```bash
kubectl get pod,svc,pvc -n mdyaipay-infra
kubectl logs -n mdyaipay-infra mysql8-0 -c mysql -f
```

**注意：** `hostpath` PVC 在删除 StatefulSet/PVC 时会丢数据；删前请保留 dump。

## 回滚到 Docker mysql8

1. `kubectl scale statefulset mysql8 -n mdyaipay-infra --replicas=0` 或删除命名空间。
2. `docker start mysql8`
3. 必要时用迁移脚本生成的 `.sql` 再导入 Docker 实例。
4. 将 compose JDBC 改回 `mysql8:3306` 并恢复 external network `mysql_default`（见 git 历史）。

设计说明：`docs/superpowers/specs/2026-09-21-mysql8-k8s-design.md`

同命名空间 Redis 见 [redis7-docker-desktop.md](redis7-docker-desktop.md)。
