#!/usr/bin/env bash
# 将 docs/monitoring 同步到 K8s 清单目录并 apply + Prometheus reload
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

K8S_MON="$ROOT/kubernetes/infra/monitoring"
MDY="$ROOT/docs/monitoring"

mdyaipay_k8s_require || exit 1

cp "$MDY/prometheus/ratelimit-recording-rules.yaml" "$K8S_MON/prometheus/rules/"
cp "$MDY/prometheus/ratelimit-alerts.yaml" "$K8S_MON/prometheus/rules/"
cp "$MDY/grafana/ratelimit-gateway-dashboard.json" "$K8S_MON/grafana/dashboards/ratelimit-gateway.json"

echo "==> 应用 K8s 监控清单"
kubectl apply -k "$K8S_MON"

if curl -sf -X POST "http://127.0.0.1:9090/-/reload" >/dev/null 2>&1; then
  echo "Prometheus 已 reload"
else
  echo "Prometheus 未响应 reload（若未部署: ./scripts/apply-mdyaipay-monitoring-k8s.sh）" >&2
  exit 1
fi

echo "Grafana: http://127.0.0.1:3000/d/mdyaipay-ratelimit-gateway"
