#!/usr/bin/env bash
# 将 mdyaipay 限流监控模板同步到本地 pay-settle 监控栈并 reload Prometheus。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MON="${MONITORING_ROOT:-$ROOT/../../ai-pay-settle/docs/docker/monitoring}"
MDY="$ROOT/docs/monitoring"
if [[ ! -d "$MON" ]]; then
  echo "未找到监控目录: $MON（可设 MONITORING_ROOT）" >&2
  exit 1
fi
cp "$MDY/prometheus/ratelimit-recording-rules.yaml" "$MON/prometheus/mdyaipay-ratelimit-recording-rules.yaml"
cp "$MDY/prometheus/ratelimit-alerts.yaml" "$MON/prometheus/mdyaipay-ratelimit-alerts.yaml"
cp "$MDY/prometheus/scrape-config.example.yaml" "$MON/prometheus/mdyaipay-scrape-config.example.yaml"
cp "$MDY/grafana/ratelimit-gateway-dashboard.json" "$MON/grafana/dashboards/mdyaipay-ratelimit-gateway.json"
docker compose -f "$MON/docker-compose.yml" up -d prometheus grafana
curl -sf -X POST http://127.0.0.1:9090/-/reload >/dev/null
echo "已同步；Grafana: http://127.0.0.1:3000/d/mdyaipay-ratelimit-gateway"
