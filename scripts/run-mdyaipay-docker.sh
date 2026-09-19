#!/usr/bin/env bash
# 本地 Docker 启动 user / payment / gateway（复用已运行的 mysql8、zk1）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT/docker/services/docker-compose.yml"
JAR_DIR="$ROOT/docker/services/jars"
VERSION="${MDYAIPAY_VERSION:-1.0.0-SNAPSHOT}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 未安装或不在 PATH" >&2
    exit 1
  fi
}

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

require_cmd docker
require_cmd mvn
require_cmd curl

if ! docker info >/dev/null 2>&1; then
  echo "Docker 未运行" >&2
  exit 1
fi

if ! docker inspect -f '{{.State.Health.Status}}' mysql8 2>/dev/null | grep -q healthy; then
  echo "mysql8 未就绪，请先启动 MySQL 容器" >&2
  exit 1
fi
if ! docker inspect -f '{{.State.Health.Status}}' zk1 2>/dev/null | grep -q healthy; then
  echo "zk1 未就绪，请先启动 ZooKeeper 容器" >&2
  exit 1
fi

echo "package user / payment / gateway ..."
mvn -f "$ROOT/pom.xml" -pl mdyaipay-user,mdyaipay-payment,mdyaipay-gateway -am package -DskipTests

mkdir -p "$JAR_DIR"
cp "$ROOT/mdyaipay-user/target/mdyaipay-user-${VERSION}-boot.jar" "$JAR_DIR/user.jar"
cp "$ROOT/mdyaipay-payment/target/mdyaipay-payment-${VERSION}-boot.jar" "$JAR_DIR/payment.jar"
cp "$ROOT/mdyaipay-gateway/target/mdyaipay-gateway-${VERSION}-boot.jar" "$JAR_DIR/gateway.jar"

echo "docker compose up ..."
docker compose -f "$COMPOSE_FILE" up -d --build

wait_health "http://127.0.0.1:8082/actuator/health" user
wait_health "http://127.0.0.1:8081/actuator/health" payment
wait_health "http://127.0.0.1:8041/actuator/health" gateway

echo ""
echo "user     http://127.0.0.1:8082"
echo "payment  http://127.0.0.1:8081"
echo "gateway  http://127.0.0.1:8041"
echo "日志: docker compose -f $COMPOSE_FILE logs -f"
