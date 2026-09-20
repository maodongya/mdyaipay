#!/usr/bin/env bash
# 在 Docker Desktop Kubernetes 部署 ZooKeeper 3 节点（mdyaipay-infra 基础设施组）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

mdyaipay_k8s_require || exit 1

if ! kubectl get namespace "$MDYAIPAY_K8S_ZK_NAMESPACE" >/dev/null 2>&1; then
  kubectl apply -f "$ROOT/kubernetes/mysql/namespace.yaml"
fi

echo "==> apply ZooKeeper 集群（namespace=$MDYAIPAY_K8S_ZK_NAMESPACE）"
kubectl apply -k "$ROOT/kubernetes/infra/zookeeper"

mdyaipay_wait_k8s_zookeeper_ready 3 120 || exit 1

echo "等待 LoadBalancer / 127.0.0.1:2181 ruok..."
if mdyaipay_wait_localhost_zookeeper 127.0.0.1 2181 90; then
  echo "ZooKeeper 已在 127.0.0.1:2181 就绪"
else
  echo "集群 Pod Ready，但 127.0.0.1:2181 未响应 imok" >&2
  exit 1
fi

echo ""
echo "Dubbo: zookeeper://host.docker.internal:2181"
echo "集群内: zookeeper.${MDYAIPAY_K8S_ZK_NAMESPACE}.svc.cluster.local:2181"
echo "自 Docker 迁移: ./scripts/migrate-zookeeper-docker-to-k8s.sh"
