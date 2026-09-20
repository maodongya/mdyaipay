#!/usr/bin/env bash
# Docker Compose 业务容器 → Kubernetes mdyaipay 命名空间
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-docker-lib.sh
source "$ROOT/scripts/mdyaipay-docker-lib.sh"

echo "==> 1/2 停止 Docker 业务容器（释放 8041/808x 等端口）"
if command -v docker >/dev/null 2>&1 && mdyaipay_run_with_timeout 8 docker info >/dev/null 2>&1; then
  COMPOSE_FILE="$ROOT/docker/services/docker-compose.yml"
  mdyaipay_run_with_timeout 60 docker compose -f "$COMPOSE_FILE" stop sentinel-dashboard "${MDYAIPAY_APP_SERVICES[@]}" 2>/dev/null || true
  mdyaipay_run_with_timeout 60 docker compose -f "$COMPOSE_FILE" rm -sf sentinel-dashboard "${MDYAIPAY_APP_SERVICES[@]}" 2>/dev/null || true
  for name in "${MDYAIPAY_LEGACY_CONTAINERS[@]}" "${MDYAIPAY_APP_SERVICES[@]}" mdyaipay-sentinel-dashboard; do
    docker rm -f "$name" 2>/dev/null || true
  done
  echo "Docker 业务容器已清理"
else
  echo "Docker 不可用，跳过 stop"
fi

echo "==> 2/2 部署 K8s 业务栈"
exec "$ROOT/scripts/run-mdyaipay-k8s.sh"
