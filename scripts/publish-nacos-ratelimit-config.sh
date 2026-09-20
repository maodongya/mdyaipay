#!/usr/bin/env bash
# 将 docs/nacos/examples 下的限流配置发布到本机 Nacos
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NACOS="${NACOS_SERVER_ADDR:-127.0.0.1:8848}"
GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
BASE="http://${NACOS}/nacos/v1/cs/configs"

publish() {
  local data_id=$1 file=$2
  echo "发布 $data_id ..."
  curl -sf -X POST "$BASE" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@${file}" >/dev/null
}

publish "mdyaipay-gateway-ratelimit.yaml" "$ROOT/docs/nacos/examples/mdyaipay-gateway-ratelimit.yaml"
publish "mdyaipay-user-ratelimit.yaml" "$ROOT/docs/nacos/examples/mdyaipay-user-ratelimit.yaml"
publish "mdyaipay-payment-ratelimit.yaml" "$ROOT/docs/nacos/examples/mdyaipay-payment-ratelimit.yaml"

echo "完成。控制台: http://${NACOS}/nacos"
