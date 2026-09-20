#!/usr/bin/env bash
# 在 Docker Desktop Kubernetes 部署 Redis 7（mdyaipay-infra / redis7）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

REDIS_DIR="$ROOT/kubernetes/redis"

mdyaipay_k8s_require || exit 1

if ! kubectl get namespace "$MDYAIPAY_K8S_REDIS_NAMESPACE" >/dev/null 2>&1; then
  kubectl apply -f "$ROOT/kubernetes/mysql/namespace.yaml"
fi

echo "==> apply Redis 7（namespace=$MDYAIPAY_K8S_REDIS_NAMESPACE）"

kubectl apply -f "$REDIS_DIR/service.yaml"
kubectl apply -f "$REDIS_DIR/statefulset.yaml"

mdyaipay_wait_k8s_redis_ready 90 || exit 1

echo "等待 LoadBalancer / 127.0.0.1:6379 可连..."
if mdyaipay_wait_localhost_redis 127.0.0.1 6379 90; then
  echo "Redis 已在 127.0.0.1:6379 就绪"
else
  echo "Pod 已 Ready，但 127.0.0.1:6379 暂不可连；请检查: kubectl get svc -n $MDYAIPAY_K8S_REDIS_NAMESPACE redis7" >&2
  exit 1
fi

echo ""
echo "集群内: redis://redis7.${MDYAIPAY_K8S_REDIS_NAMESPACE}.svc.cluster.local:6379"
echo "宿主机 / Docker: redis://host.docker.internal:6379"
echo "自 Docker 迁移 RDB: ./scripts/migrate-redis7-docker-to-k8s.sh"
