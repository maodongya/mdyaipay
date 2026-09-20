#!/usr/bin/env bash
# 停止 mdyaipay 命名空间内业务 Deployment（保留 mdyaipay-infra）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

mdyaipay_k8s_require || exit 1

if ! kubectl get namespace mdyaipay >/dev/null 2>&1; then
  echo "命名空间 mdyaipay 不存在"
  exit 0
fi

echo "==> 缩容 mdyaipay 业务 Deployment"
for d in sentinel-dashboard mdyaipay-user-1 mdyaipay-user-2 mdyaipay-payment-1 mdyaipay-payment-2 mdyaipay-gateway-1 mdyaipay-gateway-2; do
  if kubectl get deployment "$d" -n mdyaipay >/dev/null 2>&1; then
    kubectl scale "deployment/$d" -n mdyaipay --replicas=0
  fi
done

echo "完成（infra 未动；恢复: ./scripts/run-mdyaipay-k8s.sh）"
