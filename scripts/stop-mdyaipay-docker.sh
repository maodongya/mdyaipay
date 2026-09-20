#!/usr/bin/env bash
# 停止并删除 gateway / user / payment 的 Docker 容器（双节点 + 旧版单节点名）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-docker-lib.sh
source "$ROOT/scripts/mdyaipay-docker-lib.sh"
COMPOSE_FILE="$ROOT/docker/services/docker-compose.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker 未安装或不在 PATH" >&2
  exit 1
fi

echo "==> 停止并删除 mdyaipay gateway / user / payment 容器"

mdyaipay_wait_docker || exit 1

echo "compose stop / rm ..."
mdyaipay_run_with_timeout 60 docker compose -f "$COMPOSE_FILE" stop "${MDYAIPAY_APP_SERVICES[@]}" 2>/dev/null || true
mdyaipay_run_with_timeout 60 docker compose -f "$COMPOSE_FILE" rm -sf "${MDYAIPAY_APP_SERVICES[@]}" 2>/dev/null || true

remove_one() {
  local name=$1
  if ! mdyaipay_run_with_timeout 8 docker inspect "$name" >/dev/null 2>&1; then
    return 0
  fi
  echo "删除容器 $name ..."
  if mdyaipay_run_with_timeout 20 docker rm -f "$name"; then
    echo "  已删除 $name"
    return 0
  fi
  echo "  删除 $name 失败（可先 stop 或重启 Docker Desktop）" >&2
  return 1
}

failed=0
for name in "${MDYAIPAY_LEGACY_CONTAINERS[@]}" "${MDYAIPAY_APP_SERVICES[@]}"; do
  remove_one "$name" || failed=1
done

remaining="$(mdyaipay_run_with_timeout 15 docker ps -a --filter name=mdyaipay --format '{{.Names}}' 2>/dev/null \
  | grep -E 'gateway|user|payment' || true)"
if [[ -n "$remaining" ]]; then
  echo "仍存在以下容器:" >&2
  echo "$remaining" >&2
  failed=1
else
  echo "gateway / user / payment 相关容器已清理（Sentinel Dashboard 未动，见 docker/services compose）"
fi

exit "$failed"
