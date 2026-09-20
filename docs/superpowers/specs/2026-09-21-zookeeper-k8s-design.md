# ZooKeeper 3 节点集群迁移至 Docker Desktop Kubernetes

> **组：** 与 MySQL 8、Redis 7 同命名空间 **`mdyaipay-infra`**，标签 `app.kubernetes.io/part-of: mdyaipay`、`app.kubernetes.io/component: infra`。  
> **镜像：** 官方 `zookeeper:3.9.3`（对齐原 `ai-pay/zookeeper:3.9.3-min` 版本与 JVM/4lw 配置）。

---

## 1. 目标

- StatefulSet **`zookeeper`**，`replicas: 3`，headless DNS 组成 ensemble。
- 客户端 Service **`zookeeper`** LoadBalancer **:2181** → `127.0.0.1:2181` / `host.docker.internal:2181`。
- Dubbo：`DUBBO_REGISTRY_ADDRESS=zookeeper://host.docker.internal:2181`（连任一成员即可发现集群）。
- Kustomize 入口 **`kubernetes/infra/kustomization.yaml`** 聚合 mysql / redis / zookeeper。

## 2. 迁移

- 各 Docker `zk1`–`zk3` 的 `/data` 打包 → 对应 `zookeeper-0`–`2` PVC。
- 停止 Docker 三节点，释放 2181–2183。

## 3. 非目标

- 跨命名空间 ZK；生产级 PodDisruptionBudget / anti-affinity（本地 YAGNI）。
