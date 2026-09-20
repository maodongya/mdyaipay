#!/usr/bin/env bash
# 启动 user / payment / gateway（若未就绪），造压测商户，跑加密收单压测。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export DUBBO_REGISTRY_ADDRESS="${DUBBO_REGISTRY_ADDRESS:-zookeeper://127.0.0.1:2181}"
stop_local_services() {
  for port in 8081 8082 8041 20881 20882; do
    lsof -ti:"$port" 2>/dev/null | xargs kill -9 2>/dev/null || true
  done
  rm -f "$HOME/.dubbo/dubbo-registry-"*127.0.0.1-2181.cache 2>/dev/null || true
}
stop_local_services
sleep 2

export PAYMENT_BASE_URL="${PAYMENT_BASE_URL:-}"
export MDYAIPAY_RATELIMIT_BACKEND="${MDYAIPAY_RATELIMIT_BACKEND:-redisson}"
export MDYAIPAY_RATELIMIT_REDIS_URI="${MDYAIPAY_RATELIMIT_REDIS_URI:-redis://127.0.0.1:6379}"
echo "installing gateway + ratelimit modules to local Maven repo ..."
mvn -q -pl mdyaipay-gateway -am install -DskipTests

LOG_DIR="$ROOT/target/local-services"
CRED_FILE="$ROOT/target/loadtest-merchant-credentials.json"
mkdir -p "$LOG_DIR"

wait_health() {
  local url=$1 name=$2
  for i in $(seq 1 90); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "$name ready"
      return 0
    fi
    sleep 2
  done
  echo "$name not healthy: $url" >&2
  return 1
}

# shellcheck source=skywalking-agent-env.sh
source "$ROOT/scripts/skywalking-agent-env.sh"

if [[ "${SKYWALKING_START_OAP:-0}" == "1" ]]; then
  "$ROOT/scripts/run-skywalking.sh"
fi

# ENABLE_SKYWALKING: 1=强制开 Agent；0=强制关；未设且 OAP:11800 可达且 Agent 存在则自动开
skywalking_loadtest_enabled() {
  case "${ENABLE_SKYWALKING:-auto}" in
    1|true|yes) return 0 ;;
    0|false|no) return 1 ;;
    auto)
      nc -z 127.0.0.1 11800 2>/dev/null && skywalking_require_agent "$ROOT" 2>/dev/null
      ;;
    *) return 1 ;;
  esac
}

SKYWALKING_FOR_LOADTEST=0
if skywalking_loadtest_enabled; then
  if ! skywalking_require_agent "$ROOT" 2>/dev/null; then
    echo "下载 SkyWalking Java Agent ..."
    "$ROOT/scripts/download-skywalking-agent.sh"
    skywalking_require_agent "$ROOT"
  fi
  SKYWALKING_FOR_LOADTEST=1
  echo "SkyWalking Agent 已启用（上报 ${SW_AGENT_COLLECTOR_BACKEND_SERVICES:-127.0.0.1:11800}，UI http://127.0.0.1:8090）"
elif [[ "${ENABLE_SKYWALKING:-auto}" == "auto" ]]; then
  echo "未启用 SkyWalking：OAP 11800 不可达或缺少 Agent（先 ./scripts/run-skywalking.sh 与 download-skywalking-agent.sh）"
  echo "说明：mdyaipay-tools-trace 只做日志/传播，不上报 OAP；APM 须 -javaagent。"
fi

start_if_needed() {
  local port=$1 name=$2 module=$3 log=$4
  shift 4
  if curl -sf "http://127.0.0.1:${port}/actuator/health" >/dev/null 2>&1; then
    echo "$name already on :$port"
    return 0
  fi
  echo "starting $name ..."
  local mvn_args=(-q spring-boot:run)
  local sw_env=()
  if [[ "$SKYWALKING_FOR_LOADTEST" == "1" ]] && declare -f skywalking_jvm_arguments >/dev/null 2>&1; then
    local agent_id="mdyaipay-${name}::${SW_AGENT_ENV:-local}"
    local jvm
    jvm="$(skywalking_jvm_arguments "$ROOT" "$agent_id")"
    mvn_args+=(-Dspring-boot.run.jvmArguments="$jvm")
    sw_env=(SW_AGENT_NAME="$agent_id" \
      SW_AGENT_COLLECTOR_BACKEND_SERVICES="${SW_AGENT_COLLECTOR_BACKEND_SERVICES:-127.0.0.1:11800}")
    echo "  (+ SkyWalking $agent_id)"
  fi
  if ((${#sw_env[@]} > 0)); then
    (cd "$module" && env "${sw_env[@]}" "$@" mvn "${mvn_args[@]}" >"$log" 2>&1) &
  else
    (cd "$module" && env "$@" mvn "${mvn_args[@]}" >"$log" 2>&1) &
  fi
  echo $! >> "$LOG_DIR/pids.txt"
}

: > "$LOG_DIR/pids.txt"
start_if_needed 8082 user "$ROOT/mdyaipay-user" "$LOG_DIR/user.log" DUBBO_PORT=20882
start_if_needed 8081 payment "$ROOT/mdyaipay-payment" "$LOG_DIR/payment.log" DUBBO_PORT=20881
start_if_needed 8041 gateway "$ROOT/mdyaipay-gateway" "$LOG_DIR/gateway.log" \
  PAYMENT_BASE_URL="$PAYMENT_BASE_URL"

wait_health "http://127.0.0.1:8082/actuator/health" user
wait_health "http://127.0.0.1:8081/actuator/health" payment
wait_health "http://127.0.0.1:8041/actuator/health" gateway

echo "waiting for Dubbo providers to settle ..."
sleep 15

python3 "$ROOT/scripts/seed-loadtest-merchant.py"
test -f "$CRED_FILE"
export LOADTEST_MERCHANT_CREDENTIALS_FILE="$CRED_FILE"

SCENARIO="${1:-mdyaipay-tools/mdyaipay-tools-loadtest/scenarios/payment-collect-smoke.yaml}"
SCENARIO_ABS="$ROOT/$SCENARIO"
echo "loadtest scenario: $SCENARIO_ABS"
# exec:java 子进程可能读不到 shell 环境变量，场景 YAML 内 credentialsFile 指向 CRED_FILE
mvn -q -f "$ROOT/mdyaipay-tools/mdyaipay-tools-loadtest/mdyaipay-tools-loadtest-cli/pom.xml" \
  -am exec:java \
  -Dexec.args="$SCENARIO_ABS" \
  -Dexec.workingdir="$ROOT"
