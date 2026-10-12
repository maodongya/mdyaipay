#!/usr/bin/env bash
# 在 Docker Desktop Kubernetes 部署 Elasticsearch + Kibana + Filebeat 与报警 Bootstrap
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

ALERT_PROFILE="${ALERT_PROFILE:-demo}"
WEBHOOK_URL="${MDYAIPAY_ALERT_WEBHOOK_URL:-}"

mdyaipay_k8s_require || exit 1

if [[ "$ALERT_PROFILE" != "demo" && "$ALERT_PROFILE" != "production" ]]; then
  echo "ALERT_PROFILE 只能是 demo 或 production" >&2
  exit 1
fi

if ! kubectl get namespace mdyaipay-infra >/dev/null 2>&1; then
  kubectl apply -f "$ROOT/kubernetes/mysql/namespace.yaml"
fi
if ! kubectl get namespace mdyaipay >/dev/null 2>&1; then
  kubectl apply -f "$ROOT/kubernetes/apps/namespace.yaml"
fi

echo "==> upsert Kibana secrets（不打印密钥）"
ENCRYPTION_KEY=""
if kubectl get secret mdyaipay-kibana-secrets -n mdyaipay-infra >/dev/null 2>&1; then
  ENCRYPTION_KEY="$(kubectl get secret mdyaipay-kibana-secrets -n mdyaipay-infra -o jsonpath='{.data.ENCRYPTION_KEY}' | base64 --decode)"
  if [[ -z "$WEBHOOK_URL" ]]; then
    WEBHOOK_URL="$(kubectl get secret mdyaipay-kibana-secrets -n mdyaipay-infra -o jsonpath='{.data.WEBHOOK_URL}' | base64 --decode || true)"
  fi
fi
if [[ -z "$ENCRYPTION_KEY" ]]; then
  ENCRYPTION_KEY="$(openssl rand -hex 32)"
fi

kubectl create secret generic mdyaipay-kibana-secrets \
  --namespace=mdyaipay-infra \
  --from-literal=ENCRYPTION_KEY="$ENCRYPTION_KEY" \
  --from-literal=WEBHOOK_URL="$WEBHOOK_URL" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap kibana-alert-profile \
  --namespace=mdyaipay-infra \
  --from-literal=ALERT_PROFILE="$ALERT_PROFILE" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl delete job kibana-alert-bootstrap -n mdyaipay-infra --ignore-not-found

echo "==> apply Elastic Stack（namespace=mdyaipay-infra）"
kubectl apply -k "$ROOT/kubernetes/infra/elastic"
kubectl apply -f "$ROOT/kubernetes/apps/log-pipeline-heartbeat.yaml"

mdyaipay_wait_k8s_elastic_ready || exit 1
mdyaipay_wait_k8s_kibana_bootstrap || exit 1

echo ""
echo "Elasticsearch: http://127.0.0.1:9200"
echo "Kibana:        http://127.0.0.1:5601"
echo "规则 profile:  ${ALERT_PROFILE}"
echo "Webhook:       ${WEBHOOK_URL:+已配置}${WEBHOOK_URL:-未配置（仅 Server Log）}"
echo "本地安全关闭认证，仅用于 Docker Desktop。"
