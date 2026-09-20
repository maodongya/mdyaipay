#!/usr/bin/env bash
# 在 Kubernetes 部署 mdyaipay 业务（Sentinel + user/payment/gateway 各 2 节点）；依赖 mdyaipay-infra
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"
# shellcheck source=mdyaipay-docker-lib.sh
source "$ROOT/scripts/mdyaipay-docker-lib.sh"
# shellcheck source=mdyaipay-app-build.sh
source "$ROOT/scripts/mdyaipay-app-build.sh"

PASSWORD="${MDYAIPAY_MYSQL_ROOT_PASSWORD:-123456}"
SKIP_BUILD="${MDYAIPAY_SKIP_BUILD:-false}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "$1 未安装或不在 PATH" >&2
    exit 1
  }
}

wait_health() {
  local url=$1 name=$2
  for _ in $(seq 1 90); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "$name ready"
      return 0
    fi
    sleep 2
  done
  echo "$name not healthy: $url" >&2
  return 1
}

rollout() {
  kubectl rollout status "deployment/$1" -n mdyaipay --timeout=300s
}

require_cmd docker
require_cmd mvn
require_cmd curl
require_cmd kubectl

mdyaipay_k8s_require || exit 1
mdyaipay_wait_k8s_mysql_ready 20 || {
  echo "请先: ./scripts/rebuild-mdyaipay-infra-k8s.sh" >&2
  exit 1
}
mdyaipay_wait_k8s_redis_ready 20 || exit 1
mdyaipay_wait_k8s_zookeeper_ready 3 30 || exit 1

echo "==> mdyaipay 业务栈（Kubernetes / namespace mdyaipay）"

if [[ "$SKIP_BUILD" != "true" ]]; then
  mdyaipay_wait_docker || exit 1
  mdyaipay_build_app_images "$ROOT"
fi

kubectl apply -f "$ROOT/kubernetes/apps/namespace.yaml"
kubectl create secret generic mdyaipay-jdbc \
  --namespace=mdyaipay \
  --from-literal=USER_JDBC_PASSWORD="$PASSWORD" \
  --from-literal=PAYMENT_JDBC_PASSWORD="$PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "apply 清单 ..."
kubectl apply -k "$ROOT/kubernetes/apps"

rollout sentinel-dashboard
rollout mdyaipay-user-1
rollout mdyaipay-user-2
rollout mdyaipay-payment-1
rollout mdyaipay-payment-2
rollout mdyaipay-gateway-1
rollout mdyaipay-gateway-2

wait_health "http://127.0.0.1:8082/actuator/health" "user-1"
wait_health "http://127.0.0.1:8083/actuator/health" "user-2"
wait_health "http://127.0.0.1:8081/actuator/health" "payment-1"
wait_health "http://127.0.0.1:8084/actuator/health" "payment-2"
wait_health "http://127.0.0.1:8041/actuator/health" "gateway-1"
wait_health "http://127.0.0.1:8042/actuator/health" "gateway-2"

if kubectl get deploy prometheus -n mdyaipay-infra >/dev/null 2>&1; then
  kubectl apply -k "$ROOT/kubernetes/infra/monitoring" >/dev/null 2>&1 || true
  curl -sf -X POST "http://127.0.0.1:9090/-/reload" >/dev/null 2>&1 || true
fi

echo ""
echo "user     http://127.0.0.1:8082  http://127.0.0.1:8083"
echo "payment  http://127.0.0.1:8081  http://127.0.0.1:8084"
echo "gateway  http://127.0.0.1:8041  http://127.0.0.1:8042"
echo "sentinel http://127.0.0.1:8858  (sentinel / sentinel)"
echo "停止: ./scripts/stop-mdyaipay-k8s.sh"
