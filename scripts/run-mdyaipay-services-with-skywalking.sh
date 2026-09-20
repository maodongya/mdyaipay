#!/usr/bin/env bash
# 带 SkyWalking Agent 启动 user / payment / gateway（需 OAP 11800 可达）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=skywalking-agent-env.sh
source "$ROOT/scripts/skywalking-agent-env.sh"

export DUBBO_REGISTRY_ADDRESS="${DUBBO_REGISTRY_ADDRESS:-zookeeper://127.0.0.1:2181}"
export PAYMENT_BASE_URL="${PAYMENT_BASE_URL:-}"
SW_ENV="${SW_AGENT_ENV:-local}"

if [[ "${SKYWALKING_START_OAP:-0}" == "1" ]]; then
  "$ROOT/scripts/run-skywalking.sh"
fi

if ! skywalking_require_agent "$ROOT"; then
  "$ROOT/scripts/download-skywalking-agent.sh"
  skywalking_require_agent "$ROOT"
fi

LOG_DIR="$ROOT/target/local-services"
mkdir -p "$LOG_DIR"

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

start_with_agent() {
  local port=$1 name=$2 module=$3 log=$4 agent_name=$5
  shift 5
  if curl -sf "http://127.0.0.1:${port}/actuator/health" >/dev/null 2>&1; then
    echo "$name already on :$port (未替换为 Agent 进程，请先停掉旧进程)"
    return 0
  fi
  local jvm
  jvm="$(skywalking_jvm_arguments "$ROOT" "$agent_name")"
  echo "starting $name with SkyWalking ($agent_name) ..."
  (cd "$module" && env SW_AGENT_NAME="$agent_name" \
    SW_AGENT_COLLECTOR_BACKEND_SERVICES="${SW_AGENT_COLLECTOR_BACKEND_SERVICES:-127.0.0.1:11800}" \
    "$@" mvn -q spring-boot:run \
    -Dspring-boot.run.jvmArguments="$jvm" >"$log" 2>&1) &
  echo $! >> "$LOG_DIR/pids-skywalking.txt"
}

: > "$LOG_DIR/pids-skywalking.txt"
start_with_agent 8082 user "$ROOT/mdyaipay-user" "$LOG_DIR/user.log" \
  "mdyaipay-user::$SW_ENV" DUBBO_PORT=20882
start_with_agent 8081 payment "$ROOT/mdyaipay-payment" "$LOG_DIR/payment.log" \
  "mdyaipay-payment::$SW_ENV" DUBBO_PORT=20881
start_with_agent 8041 gateway "$ROOT/mdyaipay-gateway" "$LOG_DIR/gateway.log" \
  "mdyaipay-gateway::$SW_ENV" PAYMENT_BASE_URL="$PAYMENT_BASE_URL"

wait_health "http://127.0.0.1:8082/actuator/health" user
wait_health "http://127.0.0.1:8081/actuator/health" payment
wait_health "http://127.0.0.1:8041/actuator/health" gateway

echo ""
echo "SkyWalking UI: http://127.0.0.1:8090"
echo "Agent 上报: ${SW_AGENT_COLLECTOR_BACKEND_SERVICES:-127.0.0.1:11800}"
echo "日志: $LOG_DIR/{user,payment,gateway}.log"
