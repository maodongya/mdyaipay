# ZooKeeper 集群（Docker Desktop Kubernetes）

3 节点 ensemble，归属 **[mdyaipay-infra 基础设施组](mdyaipay-infra-group.md)**（与 MySQL、Redis 同命名空间）。

## 规格

- 镜像：`zookeeper:3.9.3`
- StatefulSet `zookeeper`，副本 **3**（`zookeeper-0` … `zookeeper-2`）
- headless `zookeeper-headless`（2888/3888 仲裁）
- LoadBalancer **`zookeeper:2181`** → `127.0.0.1:2181`

## 从 Docker zk1–zk3 迁移

```bash
chmod +x scripts/apply-mdyaipay-zookeeper-k8s.sh scripts/migrate-zookeeper-docker-to-k8s.sh
./scripts/migrate-zookeeper-docker-to-k8s.sh
```

各节点 `/data`、`/datalog`  tarball 导入对应 PVC。备份：`.local/zookeeper-migrate/`。

## 仅部署空集群

```bash
./scripts/apply-mdyaipay-zookeeper-k8s.sh
```

## Dubbo 连接

| 场景 | 地址 |
|------|------|
| Docker Compose | `zookeeper://host.docker.internal:2181` |
| 宿主机 / IDE | `zookeeper://127.0.0.1:2181` |
| 同集群 Pod | `zookeeper://zookeeper.mdyaipay-infra.svc.cluster.local:2181` |

连接任一成员即可发现 ensemble（与原先 `zk1:2181` 用法一致）。

## 校验

```bash
kubectl get pod,svc,pvc -n mdyaipay-infra -l app.kubernetes.io/name=zookeeper
echo ruok | nc -w 2 127.0.0.1 2181   # 应返回 imok
```

## 回滚

1. `kubectl scale statefulset zookeeper -n mdyaipay-infra --replicas=0`
2. `docker start zk1 zk2 zk3`
3. compose 改回 `zookeeper://zk1:2181` 与 external network `zookeeper_zk-net`（见 git 历史）

设计说明：`docs/superpowers/specs/2026-09-21-zookeeper-k8s-design.md`
