# shellcheck shell=bash
# 供 run-mdyaipay-docker.sh / stop-mdyaipay-docker.sh 共用的路径与 Docker 探测（source 即可，勿直接执行）

mdyaipay_docker_root() {
  cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd
}

# Docker CLI 在 Desktop 未启动时可能长时间阻塞
mdyaipay_run_with_timeout() {
  local seconds=$1
  shift
  "$@" &
  local pid=$!
  local waited=0
  while kill -0 "$pid" 2>/dev/null; do
    if (( waited >= seconds )); then
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
      return 124
    fi
    sleep 1
    waited=$((waited + 1))
  done
  wait "$pid"
}

mdyaipay_wait_docker() {
  local max_attempts=${1:-6}
  local attempt=1
  echo "检查 Docker 守护进程..."
  while (( attempt <= max_attempts )); do
    if mdyaipay_run_with_timeout 8 docker info >/dev/null 2>&1; then
      echo "Docker 已就绪"
      return 0
    fi
    if (( attempt == 1 )); then
      echo "Docker 未响应，请先打开 Docker Desktop（约每 8s 重试一次）..."
    fi
    attempt=$((attempt + 1))
  done
  echo "Docker 未运行或多次超时无响应。若 API 报 500，请在 Docker Desktop → Troubleshoot → Restart" >&2
  return 1
}

mdyaipay_container_health() {
  local name=$1
  mdyaipay_run_with_timeout 8 docker inspect -f '{{.State.Health.Status}}' "$name" 2>/dev/null || echo missing
}

# gateway / user / payment（不含 sentinel-dashboard）
MDYAIPAY_APP_SERVICES=(
  mdyaipay-user-1 mdyaipay-user-2
  mdyaipay-payment-1 mdyaipay-payment-2
  mdyaipay-gateway-1 mdyaipay-gateway-2
)

MDYAIPAY_LEGACY_CONTAINERS=(mdyaipay-user mdyaipay-payment mdyaipay-gateway)
