#!/usr/bin/env bash
# Docker 容器 redis7 → K8s StatefulSet redis7：RDB 导出、停 Docker、写入 PVC、启动
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

DUMP_DIR="${MDYAIPAY_REDIS_DUMP_DIR:-$ROOT/.local/redis7-migrate}"
RDB_FILE="$DUMP_DIR/dump-$(date +%Y%m%d-%H%M%S).rdb"
DOCKER_CONTAINER="${MDYAIPAY_DOCKER_REDIS_CONTAINER:-redis7}"
PVC_NAME="data-${MDYAIPAY_K8S_REDIS_STATEFULSET}-0"
IMPORT_POD="redis7-rdb-import"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 未安装或不在 PATH" >&2
    exit 1
  fi
}

require_cmd docker
require_cmd kubectl

mkdir -p "$DUMP_DIR"

if docker inspect "$DOCKER_CONTAINER" >/dev/null 2>&1; then
  echo "==> 1/6 从 Docker $DOCKER_CONTAINER 触发 SAVE 并复制 dump.rdb"
  docker exec "$DOCKER_CONTAINER" redis-cli SAVE >/dev/null
  docker cp "$DOCKER_CONTAINER:/data/dump.rdb" "$RDB_FILE"
  echo "备份: $RDB_FILE ($(wc -c <"$RDB_FILE" | tr -d ' ') bytes)"
  echo "==> 2/6 停止 Docker $DOCKER_CONTAINER（释放 6379）"
  docker stop "$DOCKER_CONTAINER" >/dev/null
  echo "已停止 $DOCKER_CONTAINER"
else
  echo "未找到 Docker 容器 $DOCKER_CONTAINER；将仅部署 K8s（无 RDB 导入）" >&2
  RDB_FILE=""
fi

echo "==> 3/6 部署 K8s Redis 清单"
kubectl apply -f "$ROOT/kubernetes/mysql/namespace.yaml" 2>/dev/null || true
kubectl apply -f "$ROOT/kubernetes/redis/service.yaml"
kubectl apply -f "$ROOT/kubernetes/redis/statefulset.yaml"

if [[ -n "$RDB_FILE" && -f "$RDB_FILE" ]]; then
  echo "==> 4/6 将 RDB 写入 PVC $PVC_NAME"
  kubectl scale statefulset "$MDYAIPAY_K8S_REDIS_STATEFULSET" \
    -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --replicas=1
  for _ in $(seq 1 30); do
    if kubectl get pvc -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" "$PVC_NAME" >/dev/null 2>&1; then
      break
    fi
    sleep 2
  done
  kubectl scale statefulset "$MDYAIPAY_K8S_REDIS_STATEFULSET" \
    -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --replicas=0
  for _ in $(seq 1 60); do
    if ! kubectl get pod -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" -l app.kubernetes.io/name=redis7 2>/dev/null | grep -q redis7; then
      break
    fi
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

  kubectl wait --for=condition=Ready "pod/$IMPORT_POD" \
    -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --timeout=120s

  kubectl exec -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" "$IMPORT_POD" -- sh -c \
    'rm -rf /data/appendonlydir /data/appendonly.aof /data/dump.rdb 2>/dev/null; true'

  kubectl cp "$RDB_FILE" "$MDYAIPAY_K8S_REDIS_NAMESPACE/$IMPORT_POD:/data/dump.rdb"

  kubectl delete pod "$IMPORT_POD" -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --wait=true

  echo "==> 5/6 启动 Redis StatefulSet"
  kubectl scale statefulset "$MDYAIPAY_K8S_REDIS_STATEFULSET" \
    -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" --replicas=1
else
  echo "==> 4–5/6 跳过 RDB 导入，直接等待 Pod"
fi

mdyaipay_wait_k8s_redis_ready 90 || exit 1
mdyaipay_wait_localhost_redis 127.0.0.1 6379 90 || exit 1

echo "==> 6/6 校验"
if command -v redis-cli >/dev/null 2>&1; then
  redis-cli -h 127.0.0.1 -p 6379 ping
  redis-cli -h 127.0.0.1 -p 6379 DBSIZE
fi

echo ""
echo "迁移完成。限流 URI: redis://host.docker.internal:6379"
if [[ -n "$RDB_FILE" ]]; then
  echo "RDB 备份: $RDB_FILE"
fi
