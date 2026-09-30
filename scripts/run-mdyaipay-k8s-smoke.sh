#!/usr/bin/env bash
# K8s 全栈就绪后联调：造商户 → 网关加密收单（payment → finance-gateway → finance-mock → accounting Dubbo）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

USER_BASE="${USER_BASE_URL:-http://127.0.0.1:8082}"
GATEWAY_BASE="${GATEWAY_BASE_URL:-http://127.0.0.1:8041}"
CRED_FILE="${LOADTEST_MERCHANT_CREDENTIALS_FILE:-$ROOT/target/loadtest-merchant-credentials.json}"
SCENARIO="${1:-mdyaipay-tools/mdyaipay-tools-loadtest/scenarios/k8s-gateway-collect-smoke.yaml}"

wait_health() {
  local url=$1 name=$2
  for _ in $(seq 1 60); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "$name ready"
      return 0
    fi
    sleep 2
  done
  echo "$name not healthy: $url" >&2
  return 1
}

mdyaipay_k8s_require || exit 1

echo "==> 健康检查（LoadBalancer → localhost）"
wait_health "http://127.0.0.1:8097/actuator/health" "finance-mock-1"
wait_health "http://127.0.0.1:8087/actuator/health" "finance-gateway-1"
wait_health "$USER_BASE/actuator/health" "user-1"
wait_health "http://127.0.0.1:8081/actuator/health" "payment-1"
wait_health "http://127.0.0.1:8085/actuator/health" "accounting-1"
wait_health "$GATEWAY_BASE/actuator/health" "gateway-1"
wait_health "http://127.0.0.1:8091/actuator/health" "finance-1"

echo "waiting for Dubbo providers ..."
sleep 20

echo "==> 造 ENABLED 压测商户（user）"
USER_BASE_URL="$USER_BASE" LOADTEST_MERCHANT_CREDENTIALS_FILE="$CRED_FILE" \
  python3 "$ROOT/scripts/seed-loadtest-merchant.py"

echo "==> 网关收单冒烟（loadtest）"
SCENARIO_ABS="$ROOT/$SCENARIO"
export LOADTEST_MERCHANT_CREDENTIALS_FILE="$CRED_FILE"
mvn -q -f "$ROOT/mdyaipay-tools/mdyaipay-tools-loadtest/mdyaipay-tools-loadtest-cli/pom.xml" \
  -am exec:java \
  -Dexec.args="$SCENARIO_ABS" \
  -Dexec.workingdir="$ROOT"

echo ""
echo "联调完成：gateway collect → payment(dubbo 渠道) → finance-gateway → finance-mock；账务 Dubbo 入账见 accounting 日志。"
