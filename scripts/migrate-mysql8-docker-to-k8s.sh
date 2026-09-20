#!/usr/bin/env bash
# Docker 容器 mysql8 → K8s StatefulSet mysql8：mysqldump、停 Docker、apply、restore
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

PASSWORD="${MDYAIPAY_MYSQL_ROOT_PASSWORD:-123456}"
DUMP_DIR="${MDYAIPAY_MYSQL_DUMP_DIR:-$ROOT/.local/mysql8-migrate}"
DUMP_FILE="$DUMP_DIR/all-databases-$(date +%Y%m%d-%H%M%S).sql"
DOCKER_CONTAINER="${MDYAIPAY_DOCKER_MYSQL_CONTAINER:-mysql8}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 未安装或不在 PATH" >&2
    exit 1
  fi
}

require_cmd docker
require_cmd kubectl

if ! docker inspect "$DOCKER_CONTAINER" >/dev/null 2>&1; then
  echo "未找到 Docker 容器 $DOCKER_CONTAINER；若已迁移，可直接: ./scripts/apply-mdyaipay-mysql-k8s.sh" >&2
  exit 1
fi

mkdir -p "$DUMP_DIR"

echo "==> 1/5 从 Docker $DOCKER_CONTAINER 导出（mysqldump --all-databases）"
docker exec "$DOCKER_CONTAINER" mysqldump -uroot -p"$PASSWORD" \
  --all-databases --single-transaction --routines --events --set-gtid-purged=OFF \
  >"$DUMP_FILE"
echo "备份: $DUMP_FILE ($(wc -c <"$DUMP_FILE" | tr -d ' ') bytes)"

echo "==> 2/5 停止 Docker $DOCKER_CONTAINER（释放 3306）"
docker stop "$DOCKER_CONTAINER" >/dev/null
echo "已停止 $DOCKER_CONTAINER"

echo "==> 3/5 部署 K8s MySQL"
"$ROOT/scripts/apply-mdyaipay-mysql-k8s.sh"

POD="$(mdyaipay_k8s_mysql_pod_name)"
if [[ -z "$POD" ]]; then
  echo "未找到 K8s MySQL Pod" >&2
  exit 1
fi

echo "==> 4/5 导入到 Pod $POD"
kubectl exec -i -n "$MDYAIPAY_K8S_MYSQL_NAMESPACE" "$POD" -c mysql -- \
  mysql -uroot -p"$PASSWORD" <"$DUMP_FILE"

echo "==> 5/5 校验库"
kubectl exec -n "$MDYAIPAY_K8S_MYSQL_NAMESPACE" "$POD" -c mysql -- \
  mysql -uroot -p"$PASSWORD" -e "SHOW DATABASES LIKE 'mdyaipay_%';"

echo ""
echo "迁移完成。业务 Compose 使用 host.docker.internal:3306（见 docker/services/docker-compose.yml）"
echo "备份保留: $DUMP_FILE"
echo "回滚: 停止 K8s mysql8，重新 docker start $DOCKER_CONTAINER 并 restore 该 dump"
