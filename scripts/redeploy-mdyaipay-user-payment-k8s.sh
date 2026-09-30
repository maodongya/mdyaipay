#!/usr/bin/env bash
# 重新 apply 并 rollout user / payment / accounting / finance 渠道栈（Kubernetes namespace mdyaipay）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"
# shellcheck source=mdyaipay-app-build.sh
source "$ROOT/scripts/mdyaipay-app-build.sh"

PASSWORD="${MDYAIPAY_MYSQL_ROOT_PASSWORD:-123456}"
SKIP_BUILD="${MDYAIPAY_SKIP_BUILD:-false}"

mdyaipay_k8s_require || exit 1

if [[ "$SKIP_BUILD" != "true" ]]; then
  if ! docker info >/dev/null 2>&1; then
    echo "Docker 未运行" >&2
    exit 1
  fi
  echo "==> 构建 user / payment / accounting 镜像 ..."
  mdyaipay_build_app_images "$ROOT"
fi

kubectl apply -f "$ROOT/kubernetes/apps/namespace.yaml"
kubectl create secret generic mdyaipay-jdbc \
  --namespace=mdyaipay \
  --from-literal=USER_JDBC_PASSWORD="$PASSWORD" \
  --from-literal=PAYMENT_JDBC_PASSWORD="$PASSWORD" \
  --from-literal=ACCOUNTING_JDBC_PASSWORD="$PASSWORD" \
  --from-literal=FINANCE_JDBC_PASSWORD="$PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "==> apply user / payment / accounting / finance ..."
kubectl apply -f "$ROOT/kubernetes/apps/finance-mock.yaml"
kubectl apply -f "$ROOT/kubernetes/apps/finance-gateway.yaml"
kubectl apply -f "$ROOT/kubernetes/apps/finance.yaml"
kubectl apply -f "$ROOT/kubernetes/apps/user.yaml"
kubectl apply -f "$ROOT/kubernetes/apps/payment.yaml"
kubectl apply -f "$ROOT/kubernetes/apps/accounting.yaml"

for d in mdyaipay-finance-mock-1 mdyaipay-finance-mock-2 \
  mdyaipay-finance-gateway-1 mdyaipay-finance-gateway-2 \
  mdyaipay-finance-1 mdyaipay-finance-2 \
  mdyaipay-user-1 mdyaipay-user-2 mdyaipay-payment-1 mdyaipay-payment-2 \
  mdyaipay-accounting-1 mdyaipay-accounting-2; do
  kubectl rollout status "deployment/$d" -n mdyaipay --timeout=300s
done

echo ""
echo "finance-mock     http://127.0.0.1:8097  http://127.0.0.1:8098"
echo "finance-gateway  http://127.0.0.1:8087  http://127.0.0.1:8088"
echo "finance          http://127.0.0.1:8091  http://127.0.0.1:8092"
echo "user             http://127.0.0.1:8082  http://127.0.0.1:8083"
echo "payment          http://127.0.0.1:8081  http://127.0.0.1:8084"
echo "accounting       http://127.0.0.1:8085  http://127.0.0.1:8086"
echo "联调: ./scripts/run-mdyaipay-k8s-smoke.sh"
