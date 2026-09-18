#!/usr/bin/env bash
# 本地 Docker 启动 SkyWalking（OAP gRPC 11800，UI http://127.0.0.1:8090）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_DIR="$ROOT/docker/skywalking"
cd "$COMPOSE_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker 未安装或不在 PATH" >&2
  exit 1
fi

if curl -sf "http://127.0.0.1:12800/healthcheck" >/dev/null 2>&1; then
  echo "SkyWalking OAP 已在运行"
  echo "  UI:  http://127.0.0.1:8090"
  echo "  gRPC (Agent): 127.0.0.1:11800"
  exit 0
fi

docker compose up -d

echo "等待 OAP 就绪 (12800) ..."
for i in $(seq 1 60); do
  if curl -sf "http://127.0.0.1:12800/healthcheck" >/dev/null 2>&1; then
    echo "SkyWalking OAP 就绪"
    echo "  UI:  http://127.0.0.1:8090"
    echo "  gRPC (Agent): 127.0.0.1:11800"
    exit 0
  fi
  sleep 3
done

echo "OAP 未在预期时间内就绪，请执行: docker compose -f $COMPOSE_DIR/docker-compose.yml logs oap" >&2
exit 1
