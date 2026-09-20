#!/usr/bin/env bash
# 部署 Redis 7 并将 .local RDB 写入 PVC（无 Docker 时使用）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

RDB_FILE="${MDYAIPAY_REDIS_RESTORE_RDB:-}"
DUMP_DIR="${MDYAIPAY_REDIS_DUMP_DIR:-$ROOT/.local/redis7-migrate}"
if [[ -z "$RDB_FILE" ]]; then
  RDB_FILE="$(ls -t "$DUMP_DIR"/dump-*.rdb 2>/dev/null | head -1 || true)"
fi
PVC_NAME="data-${MDYAIPAY_K8S_REDIS_STATEFULSET}-0"
IMPORT_POD="redis7-rdb-import"

if [[ -z "$RDB_FILE" || ! -f "$RDB_FILE" ]]; then
  echo "未找到 RDB 备份（$DUMP_DIR/dump-*.rdb）" >&2
  exit 1
fi

kubectl apply -f "$ROOT/kubernetes/mysql/namespace.yaml" 2>/dev/null || true
kubectl apply -f "$ROOT/kubernetes/redis/service.yaml"
kubectl apply -f "$ROOT/kubernetes/redis/statefulset.yaml"

kubectl scale statefulset "$MDYAIPAY_K8S_REDIS_STATEFULSET" \
  -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --replicas=1
for _ in $(seq 1 30); do
  kubectl get pvc -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" "$PVC_NAME" >/dev/null 2>&1 && break
  sleep 2
done
kubectl scale statefulset "$MDYAIPAY_K8S_REDIS_STATEFULSET" \
  -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --replicas=0
for _ in $(seq 1 60); do
  [[ "$(kubectl get pod -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" -l app.kubernetes.io/name=redis7 --no-headers 2>/dev/null | wc -l | tr -d ' ')" -eq 0 ]] && break
  sleep 2
done

kubectl delete pod "$IMPORT_POD" -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --ignore-not-found=true --wait=true
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: $IMPORT_POD
  namespace: $MDYAIPAY_K8S_REDIS_NAMESPACE
spec:
  restartPolicy: Never
  containers:
    - name: import
      image: busybox:1.36
      command: ["sh", "-c", "sleep 600"]
      volumeMounts:
        - name: data
          mountPath: /data
  volumes:
    - name: data
      persistentVolumeClaim:
        claimName: $PVC_NAME
EOF
kubectl wait --for=condition=Ready "pod/$IMPORT_POD" -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --timeout=120s
kubectl exec -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" "$IMPORT_POD" -- sh -c \
  'rm -rf /data/appendonlydir /data/appendonly.aof /data/dump.rdb 2>/dev/null; true'
kubectl cp "$RDB_FILE" "$MDYAIPAY_K8S_REDIS_NAMESPACE/$IMPORT_POD:/data/dump.rdb"
kubectl delete pod "$IMPORT_POD" -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --wait=true
kubectl scale statefulset "$MDYAIPAY_K8S_REDIS_STATEFULSET" \
  -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --replicas=1
mdyaipay_wait_k8s_redis_ready 90
mdyaipay_wait_localhost_redis 127.0.0.1 6379 90
echo "Redis 已恢复: $RDB_FILE"
