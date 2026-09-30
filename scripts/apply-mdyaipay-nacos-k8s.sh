#!/usr/bin/env bash
# 在 Docker Desktop Kubernetes 部署 Nacos standalone（mdyaipay-infra）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

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

if ! kubectl get namespace "$MDYAIPAY_K8S_NACOS_NAMESPACE" >/dev/null 2>&1; then
  kubectl apply -f "$ROOT/kubernetes/mysql/namespace.yaml"
fi

stop_docker_if_port 8848

echo "==> apply Nacos（namespace=$MDYAIPAY_K8S_NACOS_NAMESPACE）"
kubectl apply -k "$ROOT/kubernetes/infra/nacos"

mdyaipay_wait_k8s_nacos_ready 120 || exit 1

echo "等待 LoadBalancer / 127.0.0.1:8848 readiness..."
if mdyaipay_wait_localhost_nacos 127.0.0.1 8848 90; then
  echo "Nacos 已在 127.0.0.1:8848 就绪"
else
  echo "Pod Ready，但 127.0.0.1:8848 未响应 readiness" >&2
  exit 1
fi

echo ""
echo "控制台: http://127.0.0.1:8848/nacos  （NACOS_AUTH_ENABLE=false）"
echo "集群内: nacos.${MDYAIPAY_K8S_NACOS_NAMESPACE}.svc.cluster.local:8848"
echo "发布限流配置: NACOS_SERVER_ADDR=127.0.0.1:8848 $ROOT/scripts/publish-nacos-ratelimit-config.sh"
