#!/usr/bin/env bash
# 在 Docker Desktop Kubernetes 部署 Prometheus + Grafana（mdyaipay-infra 组）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

GRAFANA_USER="${MDYAIPAY_GRAFANA_ADMIN_USER:-admin}"
GRAFANA_PASSWORD="${MDYAIPAY_GRAFANA_ADMIN_PASSWORD:-admin}"

mdyaipay_k8s_require || exit 1

if ! kubectl get namespace mdyaipay-infra >/dev/null 2>&1; then
  kubectl apply -f "$ROOT/kubernetes/mysql/namespace.yaml"
fi

echo "==> apply Prometheus + Grafana（namespace=mdyaipay-infra）"

kubectl create secret generic grafana-admin \
  --namespace=mdyaipay-infra \
  --from-literal=admin-user="$GRAFANA_USER" \
  --from-literal=admin-password="$GRAFANA_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -k "$ROOT/kubernetes/infra/monitoring"

mdyaipay_wait_k8s_monitoring_ready || exit 1

echo ""
echo "Prometheus: http://127.0.0.1:9090"
echo "Grafana:    http://127.0.0.1:3000  （${GRAFANA_USER} / 见 MDYAIPAY_GRAFANA_ADMIN_PASSWORD，默认 admin）"
echo "看板:       http://127.0.0.1:3000/d/mdyaipay-ratelimit-gateway"
echo "自 Docker 迁移: ./scripts/migrate-monitoring-docker-to-k8s.sh"
