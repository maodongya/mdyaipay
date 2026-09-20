#!/usr/bin/env bash
# 从零重建 mdyaipay-infra（MySQL / Redis / ZK / Prometheus / Grafana）；优先从 .local 备份恢复
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

PASSWORD="${MDYAIPAY_MYSQL_ROOT_PASSWORD:-123456}"
MYSQL_DUMP="$(ls -t "$ROOT/.local/mysql8-migrate"/all-databases-*.sql 2>/dev/null | head -1 || true)"
REDIS_RDB="$(ls -t "$ROOT/.local/redis7-migrate"/dump-*.rdb 2>/dev/null | head -1 || true)"
ZK_DUMP_DIR="${MDYAIPAY_ZK_DUMP_DIR:-$ROOT/.local/zookeeper-migrate}"

stop_docker_if_port() {
  local port=$1
  local names
  names="$(docker ps --format '{{.Names}} {{.Ports}}' 2>/dev/null | awk -v p=":$port->" '$0 ~ p {print $1}' || true)"
  if [[ -n "$names" ]]; then
    echo "停止占用 ${port} 的 Docker 容器: $names"
    # shellcheck disable=SC2086
    docker stop $names >/dev/null 2>&1 || true
  fi
}

mdyaipay_k8s_require || exit 1

echo "==> mdyaipay-infra 全量重建"
stop_docker_if_port 3306
stop_docker_if_port 6379
stop_docker_if_port 2181
stop_docker_if_port 9090
stop_docker_if_port 3000

echo "==> 1/5 MySQL 8"
"$ROOT/scripts/apply-mdyaipay-mysql-k8s.sh"
if [[ -n "$MYSQL_DUMP" && -f "$MYSQL_DUMP" ]]; then
  echo "    导入 MySQL 备份: $MYSQL_DUMP"
  kubectl exec -i -n mdyaipay-infra mysql8-0 -c mysql -- \
    mysql -uroot -p"$PASSWORD" <"$MYSQL_DUMP"
else
  echo "    无 .local/mysql8-migrate/*.sql，保留空库（应用 init-schema 可建库）" >&2
fi

echo "==> 2/5 Redis 7"
if [[ -n "$REDIS_RDB" && -f "$REDIS_RDB" ]]; then
  MDYAIPAY_REDIS_RESTORE_RDB="$REDIS_RDB" "$ROOT/scripts/restore-mdyaipay-redis-k8s.sh"
else
  "$ROOT/scripts/apply-mdyaipay-redis-k8s.sh"
fi

echo "==> 3/5 ZooKeeper 3 节点"
if [[ -f "$ZK_DUMP_DIR/zk1-data.tgz" ]]; then
  MDYAIPAY_ZK_DUMP_DIR="$ZK_DUMP_DIR" "$ROOT/scripts/restore-mdyaipay-zookeeper-k8s.sh"
else
  "$ROOT/scripts/apply-mdyaipay-zookeeper-k8s.sh"
fi

echo "==> 4/5 Prometheus + Grafana"
"$ROOT/scripts/apply-mdyaipay-monitoring-k8s.sh"

echo "==> 5/5 校验"
mdyaipay_wait_k8s_mysql_ready 30
mdyaipay_wait_localhost_mysql 127.0.0.1 3306 "$PASSWORD" 10
mdyaipay_wait_k8s_redis_ready 30
mdyaipay_wait_localhost_redis 127.0.0.1 6379 10
mdyaipay_wait_k8s_zookeeper_ready 3 60
mdyaipay_wait_localhost_zookeeper 127.0.0.1 2181 10
mdyaipay_wait_k8s_monitoring_ready 60

kubectl get pod,svc -n mdyaipay-infra
echo ""
echo "重建完成。业务栈: ./scripts/run-mdyaipay-docker.sh"
echo "Grafana: http://127.0.0.1:3000/d/mdyaipay-ratelimit-gateway"
