#!/usr/bin/env bash
# 本地 Docker：gateway / user / payment 各 2 节点 + Sentinel（MySQL / Redis / ZK 在 K8s mdyaipay-infra）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-docker-lib.sh
source "$ROOT/scripts/mdyaipay-docker-lib.sh"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"
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

if ! mdyaipay_k8s_require; then
  exit 1
fi
if ! mdyaipay_wait_k8s_mysql_ready 30; then
  echo "请先部署 K8s MySQL: ./scripts/apply-mdyaipay-mysql-k8s.sh 或迁移: ./scripts/migrate-mysql8-docker-to-k8s.sh" >&2
  exit 1
fi
PASSWORD="${MDYAIPAY_MYSQL_ROOT_PASSWORD:-123456}"
if ! mdyaipay_wait_localhost_mysql 127.0.0.1 3306 "$PASSWORD" 15; then
  echo "127.0.0.1:3306 MySQL 不可连（LoadBalancer 未就绪？）" >&2
  exit 1
fi
if ! mdyaipay_wait_k8s_redis_ready 30; then
  echo "请先部署 K8s Redis: ./scripts/apply-mdyaipay-redis-k8s.sh 或迁移: ./scripts/migrate-redis7-docker-to-k8s.sh" >&2
  exit 1
fi
if ! mdyaipay_wait_localhost_redis 127.0.0.1 6379 15; then
  echo "127.0.0.1:6379 Redis 不可连" >&2
  exit 1
fi
if ! mdyaipay_wait_k8s_zookeeper_ready 3 30; then
  echo "请先部署 K8s ZooKeeper: ./scripts/apply-mdyaipay-zookeeper-k8s.sh 或 ./scripts/migrate-zookeeper-docker-to-k8s.sh" >&2
  exit 1
fi
if ! mdyaipay_wait_localhost_zookeeper 127.0.0.1 2181 15; then
  echo "127.0.0.1:2181 ZooKeeper 未响应 ruok/imok" >&2
  exit 1
fi
echo "依赖就绪: K8s mysql8、redis7、zookeeper（127.0.0.1:3306/6379/2181）"

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
