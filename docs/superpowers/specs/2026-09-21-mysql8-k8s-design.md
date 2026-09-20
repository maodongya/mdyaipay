# MySQL 8 迁移至 Docker Desktop Kubernetes 设计

> **环境：** Docker Desktop 内置 Kubernetes（context `docker-desktop`）。  
> **数据：** 从现有 Docker 容器 `mysql8` 逻辑备份后导入 K8s PVC。  
> **范围：** 仅 MySQL 8；业务仍可在 Docker Compose 运行，经宿主机访问 K8s 中的实例。

---

## 1. 目标与非目标

### 1.1 目标

- 在命名空间 `mdyaipay-infra` 部署单副本 **StatefulSet**，镜像 `mysql:8.0`，配置与现 Docker 对齐（`my.cnf`、root 密码、时区）。
- 持久化使用默认 StorageClass **`hostpath`**。
- 对宿主机暴露 **3306**（Docker Desktop 使用 **LoadBalancer**，映射为 `127.0.0.1:3306`）。
- 提供 **apply** 与 **migrate** 脚本：dump → 停 Docker `mysql8` → 部署 → restore。
- 更新 `docker/services/docker-compose.yml` JDBC 为 `host.docker.internal:3306`；`run-mdyaipay-docker.sh` 依赖检查改为 K8s Pod Ready。

### 1.2 非目标

- MySQL 主从、Operator、云 RDS。
- ZK / Redis / 应用 Pod 上 K8s（后续迭代）。
- 将 root 密码明文提交进 Git（Secret 由脚本生成）。

---

## 2. 架构

```text
mdyaipay-infra/
  ConfigMap mysql8-mysqld-conf   → /etc/mysql/conf.d/99-mdyaipay.cnf
  Secret mysql8-root             → MYSQL_ROOT_PASSWORD
  StatefulSet mysql8 (1)
    volumeClaimTemplate data → hostpath PVC
  Service mysql8 (LoadBalancer :3306)
```

**集群内 DNS（应用日后上 K8s）：** `mysql8.mdyaipay-infra.svc.cluster.local:3306`

**Docker Compose 过渡期：** `jdbc:mysql://host.docker.internal:3306/...`

---

## 3. 配置对齐

| 项 | 值 |
|----|-----|
| 镜像 | `mysql:8.0` |
| root 密码 | 与现环境一致；环境变量 `MDYAIPAY_MYSQL_ROOT_PASSWORD`（默认本地 `123456`） |
| TZ | `Asia/Shanghai` |
| 内存 | 容器 limit/request **2Gi** |
| mysqld | 仓库 `kubernetes/mysql/conf/99-mdyaipay.cnf`（自 ai-pay-settle 调优文件 vend） |

---

## 4. 数据迁移顺序

1. `migrate-mysql8-docker-to-k8s.sh` 从运行中的 Docker `mysql8` 执行 `mysqldump`（全库或业务库）。
2. 停止 Docker `mysql8`，释放宿主机 3306。
3. `apply-mdyaipay-mysql-k8s.sh` 创建 Secret、apply 清单、等待 Pod Ready。
4. 将 dump 导入 Pod（脚本内 `kubectl exec`）。
5. 验证 `127.0.0.1:3306` 与库表；再启动 Compose 业务容器。

**回滚：** 保留 dump；重新启动 Docker `mysql8` 并 restore，compose JDBC 改回 `mysql8:3306` 与 external network `mysql_default`。

---

## 5. 仓库布局

```text
kubernetes/mysql/
  namespace.yaml
  configmap-mysqld.yaml
  statefulset.yaml
  service.yaml
  conf/99-mdyaipay.cnf
scripts/apply-mdyaipay-mysql-k8s.sh
scripts/migrate-mysql8-docker-to-k8s.sh
scripts/mdyaipay-k8s-lib.sh
docs/kubernetes/mysql8-docker-desktop.md
```

---

## 6. 风险

- **3306 冲突：** Docker 与 K8s 不可同时占用；迁移脚本强制先 dump 再停 Docker。
- **hostpath：** 删除 PVC/命名空间会丢数据；文档强调备份。
- **LoadBalancer：** 依赖 Docker Desktop 行为；其他集群需改 Service 类型或 Ingress。

---

## 7. 验收

- `kubectl get pod -n mdyaipay-infra` 中 `mysql8-0` Ready。
- `mysql -h127.0.0.1 -uroot -p` 可连，库 `mdyaipay_user`、`mdyaipay_payment` 与迁移前一致。
- `./scripts/run-mdyaipay-docker.sh` 在 K8s MySQL 就绪后可通过健康检查并启动业务容器。
