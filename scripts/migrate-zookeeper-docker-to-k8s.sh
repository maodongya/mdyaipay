#!/usr/bin/env bash
# Docker zk1/zk2/zk3 → K8s StatefulSet zookeeper（3 副本，mdyaipay-infra 组）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=mdyaipay-k8s-lib.sh
source "$ROOT/scripts/mdyaipay-k8s-lib.sh"

DUMP_DIR="${MDYAIPAY_ZK_DUMP_DIR:-$ROOT/.local/zookeeper-migrate}"
DOCKER_ZK_CONTAINERS=(zk1 zk2 zk3)

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 未安装或不在 PATH" >&2
    exit 1
  fi
}

require_cmd docker
require_cmd kubectl

mkdir -p "$DUMP_DIR"

have_docker_zk=false
for c in "${DOCKER_ZK_CONTAINERS[@]}"; do
  if docker inspect "$c" >/dev/null 2>&1; then
    have_docker_zk=true
    break
  fi
done

if $have_docker_zk; then
  echo "==> 1/6 导出各节点 /data、/datalog"
  for id in 1 2 3; do
    c="zk$id"
    if ! docker inspect "$c" >/dev/null 2>&1; then
      echo "缺少容器 $c，无法完整迁移" >&2
      exit 1
    fi
    docker exec "$c" tar czf - -C /data . >"$DUMP_DIR/zk${id}-data.tgz"
    docker exec "$c" sh -c 'test -d /datalog && tar czf - -C /datalog .' >"$DUMP_DIR/zk${id}-datalog.tgz" 2>/dev/null || true
    echo "  $c data=$(wc -c <"$DUMP_DIR/zk${id}-data.tgz" | tr -d ' ') bytes"
  done
  echo "==> 2/6 停止 Docker ZooKeeper（释放 2181–2183）"
  for c in "${DOCKER_ZK_CONTAINERS[@]}"; do
    docker stop "$c" >/dev/null 2>&1 || true
  done
  echo "已停止 zk1、zk2、zk3"
else
  echo "未找到 Docker zk1–zk3，将仅部署 K8s 空集群" >&2
fi

echo "==> 3/6 部署 K8s ZooKeeper"
kubectl apply -f "$ROOT/kubernetes/mysql/namespace.yaml" 2>/dev/null || true
kubectl apply -k "$ROOT/kubernetes/infra/zookeeper"

if $have_docker_zk; then
  echo "==> 4/6 写入各 Pod PVC"
  kubectl scale statefulset "$MDYAIPAY_K8S_ZK_STATEFULSET" \
    -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --replicas=3
  for _ in $(seq 1 60); do
    if kubectl get pvc -n "$MDYAIPAY_K8S_ZK_NAMESPACE" data-zookeeper-2 >/dev/null 2>&1; then
      break
    fi
    sleep 2
  done
  kubectl scale statefulset "$MDYAIPAY_K8S_ZK_STATEFULSET" \
    -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --replicas=0
  for _ in $(seq 1 90); do
    if [[ "$(kubectl get pod -n "$MDYAIPAY_K8S_ZK_NAMESPACE" -l app.kubernetes.io/name=zookeeper --no-headers 2>/dev/null | wc -l | tr -d ' ')" -eq 0 ]]; then
      break
    fi
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

    kubectl wait --for=condition=Ready "pod/$import_pod" \
      -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --timeout=120s

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
    echo "  已导入 zookeeper-${ord} ← zk${id}"
  done

  echo "==> 5/6 启动 3 副本"
  kubectl scale statefulset "$MDYAIPAY_K8S_ZK_STATEFULSET" \
    -n "$MDYAIPAY_K8S_ZK_NAMESPACE" --replicas=3
else
  echo "==> 4–5/6 跳过数据导入"
fi

mdyaipay_wait_k8s_zookeeper_ready 3 180 || exit 1
mdyaipay_wait_localhost_zookeeper 127.0.0.1 2181 90 || exit 1

echo "==> 6/6 校验"
if command -v nc >/dev/null 2>&1; then
  echo ruok | nc -w 2 127.0.0.1 2181
fi

echo ""
echo "迁移完成。Dubbo: zookeeper://host.docker.internal:2181"
echo "备份目录: $DUMP_DIR"
