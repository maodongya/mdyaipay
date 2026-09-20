#!/usr/bin/env bash
# 在 Docker Desktop Kubernetes 部署 MySQL 8（mdyaipay-infra / mysql8）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

MYSQL_DIR="$ROOT/kubernetes/mysql"
PASSWORD="${MDYAIPAY_MYSQL_ROOT_PASSWORD:-123456}"

mdyaipay_k8s_require || exit 1

echo "==> apply MySQL 8（namespace=$MDYAIPAY_K8S_MYSQL_NAMESPACE）"

kubectl apply -f "$MYSQL_DIR/namespace.yaml"
kubectl create secret generic mysql8-root \
  --namespace="$MDYAIPAY_K8S_MYSQL_NAMESPACE" \
  --from-literal=MYSQL_ROOT_PASSWORD="$PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -f "$MYSQL_DIR/configmap-mysqld.yaml"
kubectl apply -f "$MYSQL_DIR/service.yaml"
kubectl apply -f "$MYSQL_DIR/statefulset.yaml"

mdyaipay_wait_k8s_mysql_ready 120 || exit 1

echo "等待 LoadBalancer / 127.0.0.1:3306 可连..."
if mdyaipay_wait_localhost_mysql 127.0.0.1 3306 "$PASSWORD" 90; then
  echo "MySQL 已在 127.0.0.1:3306 就绪"
else
  echo "Pod 已 Ready，但 127.0.0.1:3306 暂不可连；请检查: kubectl get svc -n $MDYAIPAY_K8S_MYSQL_NAMESPACE mysql8" >&2
  exit 1
fi

echo ""
echo "集群内: mysql8.${MDYAIPAY_K8S_MYSQL_NAMESPACE}.svc.cluster.local:3306"
echo "宿主机 / Docker: host.docker.internal:3306 或 127.0.0.1:3306"
echo "首次自 Docker 迁移数据: ./scripts/migrate-mysql8-docker-to-k8s.sh"
