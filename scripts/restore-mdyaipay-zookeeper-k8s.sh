#!/usr/bin/env bash
# 部署 ZooKeeper 3 节点并从 .local tarball 恢复（无 Docker 时使用）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

DUMP_DIR="${MDYAIPAY_ZK_DUMP_DIR:-$ROOT/.local/zookeeper-migrate}"

for id in 1 2 3; do
  if [[ ! -f "$DUMP_DIR/zk${id}-data.tgz" ]]; then
    echo "缺少 $DUMP_DIR/zk${id}-data.tgz" >&2
    exit 1
  fi
done

kubectl apply -f "$ROOT/kubernetes/mysql/namespace.yaml" 2>/dev/null || true
kubectl apply -k "$ROOT/kubernetes/infra/zookeeper"

kubectl scale statefulset "$MDYAIPAY_K8S_ZK_STATEFULSET" \
  -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --replicas=3
for _ in $(seq 1 60); do
  kubectl get pvc -n "$MDYAIPAY_K8S_ZK_NAMESPACE" data-zookeeper-2 >/dev/null 2>&1 && break
  sleep 2
done
kubectl scale statefulset "$MDYAIPAY_K8S_ZK_STATEFULSET" \
  -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --replicas=0
for _ in $(seq 1 90); do
  [[ "$(kubectl get pod -n "$MDYAIPAY_K8S_ZK_NAMESPACE" -l app.kubernetes.io/name=zookeeper --no-headers 2>/dev/null | wc -l | tr -d ' ')" -eq 0 ]] && break
  sleep 2
done

for ord in 0 1 2; do
  id=$((ord + 1))
  import_pod="zookeeper-import-${ord}"
  data_pvc="data-zookeeper-${ord}"
  datalog_pvc="datalog-zookeeper-${ord}"
  data_tar="$DUMP_DIR/zk${id}-data.tgz"
  datalog_tar="$DUMP_DIR/zk${id}-datalog.tgz"

  kubectl delete pod "$import_pod" -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --ignore-not-found=true --wait=true
  cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: $import_pod
  namespace: $MDYAIPAY_K8S_ZK_NAMESPACE
spec:
  restartPolicy: Never
  containers:
    - name: import
      image: busybox:1.36
      command: ["sh", "-c", "sleep 900"]
      volumeMounts:
        - name: data
          mountPath: /data
        - name: datalog
          mountPath: /datalog
  volumes:
    - name: data
      persistentVolumeClaim:
        claimName: $data_pvc
    - name: datalog
      persistentVolumeClaim:
        claimName: $datalog_pvc
EOF
  kubectl wait --for=condition=Ready "pod/$import_pod" -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --timeout=120s
  kubectl exec -n "$MDYAIPAY_K8S_ZK_NAMESPACE" "$import_pod" -- sh -c \
    'rm -rf /data/* /datalog/* 2>/dev/null; true'
  mkdir -p "/tmp/zk-import-data-$ord"
  tar xzf "$data_tar" -C "/tmp/zk-import-data-$ord"
  kubectl cp "/tmp/zk-import-data-$ord/." "$MDYAIPAY_K8S_ZK_NAMESPACE/$import_pod:/data/"
  rm -rf "/tmp/zk-import-data-$ord"
  if [[ -s "$datalog_tar" ]]; then
    mkdir -p "/tmp/zk-import-datalog-$ord"
    tar xzf "$datalog_tar" -C "/tmp/zk-import-datalog-$ord" 2>/dev/null || true
    kubectl cp "/tmp/zk-import-datalog-$ord/." "$MDYAIPAY_K8S_ZK_NAMESPACE/$import_pod:/datalog/" 2>/dev/null || true
    rm -rf "/tmp/zk-import-datalog-$ord"
  fi
  kubectl delete pod "$import_pod" -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --wait=true
done

kubectl scale statefulset "$MDYAIPAY_K8S_ZK_STATEFULSET" \
  -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --replicas=3
mdyaipay_wait_k8s_zookeeper_ready 3 180
mdyaipay_wait_localhost_zookeeper 127.0.0.1 2181 90
echo "ZooKeeper 已从 $DUMP_DIR 恢复"
