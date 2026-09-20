#!/usr/bin/env bash
# Docker pay-prometheus / pay-grafana → K8s mdyaipay-infra（TSDB/Grafana 库可选保留，默认新卷）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

PROM_CONTAINER="${MDYAIPAY_DOCKER_PROMETHEUS_CONTAINER:-pay-prometheus}"
GRAF_CONTAINER="${MDYAIPAY_DOCKER_GRAFANA_CONTAINER:-pay-grafana}"

stop_if_running() {
  local name=$1
  if docker inspect "$name" >/dev/null 2>&1; then
    local running
    running="$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null)"
    if [[ "$running" == "true" ]]; then
      docker stop "$name" >/dev/null
      echo "已停止 Docker $name"
    fi
  fi
}

echo "==> 1/2 停止本地 Docker 监控容器（释放 9090 / 3000）"
stop_if_running "$PROM_CONTAINER"
stop_if_running "$GRAF_CONTAINER"

echo "==> 2/2 部署 K8s Prometheus + Grafana"
"$ROOT/scripts/apply-mdyaipay-monitoring-k8s.sh"

echo ""
echo "规则与看板源文件: docs/monitoring/（由 Kustomize configMapGenerator 挂载）"
echo "更新规则后: kubectl apply -k kubernetes/infra/monitoring && curl -X POST http://127.0.0.1:9090/-/reload"
