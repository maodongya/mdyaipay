#!/usr/bin/env bash
# 本机 Docker：Sentinel Dashboard（8858）+ Nacos standalone（8848），同一 compose 组 mdyaipay-infra
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE="$ROOT/docker/sentinel/docker-compose.yml"

if ! docker info >/dev/null 2>&1; then
  echo "Docker 未运行" >&2
  exit 1
fi

if ! docker image inspect mdyaipay-user:local >/dev/null 2>&1; then
  echo "缺少 mdyaipay-user:local，请先执行 ./scripts/run-mdyaipay-docker.sh 或 mvn 打包后构建 user 镜像" >&2
  exit 1
fi

JAR_DIR="$ROOT/docker/sentinel/jars"
VERSION="${SENTINEL_DASHBOARD_VERSION:-1.8.8}"
JAR="$JAR_DIR/sentinel-dashboard-${VERSION}.jar"
mkdir -p "$JAR_DIR"
if [[ ! -f "$JAR" ]]; then
  echo "下载 sentinel-dashboard-${VERSION}.jar ..."
  curl -fL -o "$JAR" \
    "https://github.com/alibaba/Sentinel/releases/download/${VERSION}/sentinel-dashboard-${VERSION}.jar"
fi

if ! docker image inspect "${NACOS_IMAGE:-nacos/nacos-server:v2.4.3}" >/dev/null 2>&1; then
  echo "拉取 Nacos 镜像（可设 NACOS_IMAGE，例如 docker.1ms.run/nacos/nacos-server:v2.4.3）..."
  docker compose -f "$COMPOSE" pull nacos || true
fi

echo "docker compose up（sentinel-dashboard + nacos）..."
docker compose -f "$COMPOSE" up -d --build

wait_url() {
  local url=$1 label=$2 max=${3:-40}
  local i
  for ((i = 1; i <= max; i++)); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "$label ready: $url"
      return 0
    fi
    sleep 2
  done
  return 1
}

ok=0
wait_url "http://127.0.0.1:8858/" "Sentinel Dashboard" 30 && ok=$((ok + 1)) || \
  echo "Sentinel 未就绪，日志: docker compose -f $COMPOSE logs sentinel-dashboard" >&2
wait_url "http://127.0.0.1:8848/nacos/v1/console/health/readiness" "Nacos" 45 && ok=$((ok + 1)) || \
  echo "Nacos 未就绪，日志: docker compose -f $COMPOSE logs nacos" >&2

echo ""
echo "Sentinel  http://127.0.0.1:8858   账号 sentinel / sentinel"
echo "Nacos     http://127.0.0.1:8848/nacos   默认 nacos / nacos（当前 NACOS_AUTH_ENABLE=false）"
echo "Dubbo 注册示例: nacos://127.0.0.1:8848"
echo "日志: docker compose -f $COMPOSE logs -f"

[[ "$ok" -ge 1 ]] || exit 1
