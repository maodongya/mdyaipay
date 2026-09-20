#!/usr/bin/env bash
# 本地 Docker：gateway / user / payment 各 2 节点 + Sentinel Dashboard（复用 mysql8、zk1）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-docker-lib.sh
source "$ROOT/scripts/mdyaipay-docker-lib.sh"
COMPOSE_FILE="$ROOT/docker/services/docker-compose.yml"
JAR_DIR="$ROOT/docker/services/jars"
VERSION="${MDYAIPAY_VERSION:-1.0.0-SNAPSHOT}"

COMPOSE_SERVICES=(sentinel-dashboard "${MDYAIPAY_APP_SERVICES[@]}")

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

echo "==> mdyaipay Docker：gateway / user / payment 各 2 节点"

mdyaipay_wait_docker || exit 1

mysql_health="$(mdyaipay_container_health mysql8)"
if [[ "$mysql_health" != healthy ]]; then
  echo "mysql8 未就绪（当前: ${mysql_health}），请先启动 MySQL 容器" >&2
  exit 1
fi
zk_health="$(mdyaipay_container_health zk1)"
if [[ "$zk_health" != healthy ]]; then
  echo "zk1 未就绪（当前: ${zk_health}），请先启动 ZooKeeper 容器" >&2
  exit 1
fi
echo "依赖就绪: mysql8、zk1"

if ! nc -z 127.0.0.1 6379 2>/dev/null; then
  echo "警告: 127.0.0.1:6379 无 Redis（请先启动 redis7 等），user/payment/gateway 会因 Redisson 连不上而退出" >&2
fi

for legacy in "${MDYAIPAY_LEGACY_CONTAINERS[@]}"; do
  if mdyaipay_run_with_timeout 5 docker inspect "$legacy" >/dev/null 2>&1; then
    echo "移除旧版单节点容器 $legacy（避免与双节点端口冲突）..."
    docker rm -f "$legacy" >/dev/null
  fi
done

echo "package user / payment / gateway ..."
mvn -f "$ROOT/pom.xml" -pl mdyaipay-user,mdyaipay-payment,mdyaipay-gateway -am package -DskipTests

mkdir -p "$JAR_DIR"
cp "$ROOT/mdyaipay-user/target/mdyaipay-user-${VERSION}-boot.jar" "$JAR_DIR/user.jar"
cp "$ROOT/mdyaipay-payment/target/mdyaipay-payment-${VERSION}-boot.jar" "$JAR_DIR/payment.jar"
cp "$ROOT/mdyaipay-gateway/target/mdyaipay-gateway-${VERSION}-boot.jar" "$JAR_DIR/gateway.jar"

echo "docker compose up（${#COMPOSE_SERVICES[@]} 个服务，user/payment/gateway 各 2 节点）..."
docker compose -f "$COMPOSE_FILE" up -d --build "${COMPOSE_SERVICES[@]}"

wait_health "http://127.0.0.1:8082/actuator/health" "user-1"
wait_health "http://127.0.0.1:8083/actuator/health" "user-2"
wait_health "http://127.0.0.1:8081/actuator/health" "payment-1"
wait_health "http://127.0.0.1:8084/actuator/health" "payment-2"
wait_health "http://127.0.0.1:8041/actuator/health" "gateway-1"
wait_health "http://127.0.0.1:8042/actuator/health" "gateway-2"

echo ""
echo "user     http://127.0.0.1:8082 (node-1)  http://127.0.0.1:8083 (node-2)"
echo "payment  http://127.0.0.1:8081 (node-1)  http://127.0.0.1:8084 (node-2)"
echo "gateway  http://127.0.0.1:8041 (node-1)  http://127.0.0.1:8042 (node-2)"
echo "sentinel http://127.0.0.1:8858  (默认账号 sentinel / sentinel)"
echo "Dubbo 经 ZK 负载；HTTP 压测可任选一 gateway 节点（8041/8042）"
echo "停止业务容器: ./scripts/stop-mdyaipay-docker.sh"
echo "日志: docker compose -f $COMPOSE_FILE logs -f"
