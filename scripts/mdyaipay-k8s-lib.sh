# shellcheck shell=bash
# kubectl 上下文、MySQL / Redis K8s 就绪探测（供 apply / migrate / run-mdyaipay-docker 共用）

MDYAIPAY_K8S_INFRA_NAMESPACE="${MDYAIPAY_K8S_INFRA_NAMESPACE:-mdyaipay-infra}"
MDYAIPAY_K8S_MYSQL_NAMESPACE="${MDYAIPAY_K8S_MYSQL_NAMESPACE:-$MDYAIPAY_K8S_INFRA_NAMESPACE}"
MDYAIPAY_K8S_REDIS_NAMESPACE="${MDYAIPAY_K8S_REDIS_NAMESPACE:-$MDYAIPAY_K8S_INFRA_NAMESPACE}"
MDYAIPAY_K8S_MYSQL_STATEFULSET="${MDYAIPAY_K8S_MYSQL_STATEFULSET:-mysql8}"
MDYAIPAY_K8S_MYSQL_SERVICE="${MDYAIPAY_K8S_MYSQL_SERVICE:-mysql8}"

mdyaipay_k8s_require() {
  if ! command -v kubectl >/dev/null 2>&1; then
    echo "kubectl 未安装或不在 PATH" >&2
    return 1
  fi
  if ! kubectl cluster-info >/dev/null 2>&1; then
    echo "Kubernetes 不可用（请启用 Docker Desktop → Kubernetes）" >&2
    return 1
  fi
  return 0
}

mdyaipay_k8s_mysql_pod_name() {
  kubectl get pod -n "$MDYAIPAY_K8S_MYSQL_NAMESPACE" \
    -l "app.kubernetes.io/name=mysql8" \
    -o jsonpath='{.items[0].metadata.name}' 2>/dev/null
}

mdyaipay_k8s_mysql_ready() {
  local pod
  pod="$(mdyaipay_k8s_mysql_pod_name)"
  if [[ -z "$pod" ]]; then
    echo "missing"
    return 1
  fi
  local ready
  ready="$(kubectl get pod -n "$MDYAIPAY_K8S_MYSQL_NAMESPACE" "$pod" \
    -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)"
  if [[ "$ready" == "True" ]]; then
    echo "ready"
    return 0
  fi
  echo "not-ready"
  return 1
}

mdyaipay_wait_k8s_mysql_ready() {
  local attempts="${1:-90}"
  local i=1
  while (( i <= attempts )); do
    if mdyaipay_k8s_mysql_ready >/dev/null; then
      echo "K8s MySQL Pod 已 Ready"
      return 0
    fi
    sleep 2
    i=$((i + 1))
  done
  echo "K8s MySQL 未在预期时间内 Ready（namespace=$MDYAIPAY_K8S_MYSQL_NAMESPACE）" >&2
  kubectl get pod -n "$MDYAIPAY_K8S_MYSQL_NAMESPACE" -l app.kubernetes.io/name=mysql8 2>&1 || true
  return 1
}

mdyaipay_wait_localhost_mysql() {
  local host="${1:-127.0.0.1}"
  local port="${2:-3306}"
  local password="${3:-}"
  local attempts="${4:-60}"
  if ! command -v mysql >/dev/null 2>&1; then
    if command -v nc >/dev/null 2>&1; then
      local i=1
      while (( i <= attempts )); do
        if nc -z "$host" "$port" 2>/dev/null; then
          return 0
        fi
        sleep 2
        i=$((i + 1))
      done
      return 1
    fi
    echo "无 mysql 客户端且 nc 不可用，跳过 TCP 探测" >&2
    return 0
  fi
  local i=1
  while (( i <= attempts )); do
    if mysql -h"$host" -P"$port" -uroot -p"$password" -e "SELECT 1" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
    i=$((i + 1))
  done
  return 1
}

MDYAIPAY_K8S_REDIS_STATEFULSET="${MDYAIPAY_K8S_REDIS_STATEFULSET:-redis7}"

mdyaipay_k8s_redis_pod_name() {
  kubectl get pod -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" \
    -l "app.kubernetes.io/name=redis7" \
    -o jsonpath='{.items[0].metadata.name}' 2>/dev/null
}

mdyaipay_k8s_redis_ready() {
  local pod
  pod="$(mdyaipay_k8s_redis_pod_name)"
  if [[ -z "$pod" ]]; then
    echo "missing"
    return 1
  fi
  local ready
  ready="$(kubectl get pod -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" "$pod" \
    -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)"
  if [[ "$ready" == "True" ]]; then
    echo "ready"
    return 0
  fi
  echo "not-ready"
  return 1
}

mdyaipay_wait_k8s_redis_ready() {
  local attempts="${1:-60}"
  local i=1
  while (( i <= attempts )); do
    if mdyaipay_k8s_redis_ready >/dev/null; then
      echo "K8s Redis Pod 已 Ready"
      return 0
    fi
    sleep 2
    i=$((i + 1))
  done
  echo "K8s Redis 未在预期时间内 Ready（namespace=$MDYAIPAY_K8S_REDIS_NAMESPACE）" >&2
  kubectl get pod -n "$MDYAIPAY_K8S_REDIS_NAMESPACE" -l app.kubernetes.io/name=redis7 2>&1 || true
  return 1
}

mdyaipay_wait_localhost_redis() {
  local host="${1:-127.0.0.1}"
  local port="${2:-6379}"
  local attempts="${3:-60}"
  if command -v redis-cli >/dev/null 2>&1; then
    local i=1
    while (( i <= attempts )); do
      if redis-cli -h "$host" -p "$port" ping 2>/dev/null | grep -q PONG; then
        return 0
      fi
      sleep 2
      i=$((i + 1))
    done
    return 1
  fi
  if command -v nc >/dev/null 2>&1; then
    local i=1
    while (( i <= attempts )); do
      if nc -z "$host" "$port" 2>/dev/null; then
        return 0
      fi
      sleep 2
      i=$((i + 1))
    done
    return 1
  fi
  echo "无 redis-cli 且 nc 不可用，跳过 Redis TCP 探测" >&2
  return 0
}

MDYAIPAY_K8S_ZK_STATEFULSET="${MDYAIPAY_K8S_ZK_STATEFULSET:-zookeeper}"
MDYAIPAY_K8S_ZK_NAMESPACE="${MDYAIPAY_K8S_ZK_NAMESPACE:-$MDYAIPAY_K8S_INFRA_NAMESPACE}"

mdyaipay_k8s_zookeeper_ready_count() {
  kubectl get pod -n "$MDYAIPAY_K8S_ZK_NAMESPACE" \
    -l "app.kubernetes.io/name=zookeeper" \
    --no-headers 2>/dev/null | awk '$2=="1/1"' | wc -l | tr -d ' '
}

mdyaipay_wait_k8s_zookeeper_ready() {
  local expected="${1:-3}"
  local attempts="${2:-120}"
  local i=1
  while (( i <= attempts )); do
    local ready
    ready="$(mdyaipay_k8s_zookeeper_ready_count)"
    if [[ "$ready" -ge "$expected" ]]; then
      echo "K8s ZooKeeper 集群 Ready（${ready}/${expected}）"
      return 0
    fi
    sleep 3
    i=$((i + 1))
  done
  echo "K8s ZooKeeper 未在预期时间内 Ready（namespace=$MDYAIPAY_K8S_ZK_NAMESPACE）" >&2
  kubectl get pod -n "$MDYAIPAY_K8S_ZK_NAMESPACE" -l app.kubernetes.io/name=zookeeper 2>&1 || true
  return 1
}

mdyaipay_wait_localhost_zookeeper() {
  local host="${1:-127.0.0.1}"
  local port="${2:-2181}"
  local attempts="${3:-60}"
  local i=1
  while (( i <= attempts )); do
    if command -v nc >/dev/null 2>&1; then
      if echo ruok | nc -w 2 "$host" "$port" 2>/dev/null | grep -q imok; then
        return 0
      fi
    fi
    sleep 2
    i=$((i + 1))
  done
  return 1
}

mdyaipay_wait_k8s_monitoring_ready() {
  local attempts="${1:-90}"
  local i=1
  while (( i <= attempts )); do
    local prom_ready grafana_ready
    prom_ready="$(kubectl get deploy prometheus -n mdyaipay-infra -o jsonpath='{.status.readyReplicas}' 2>/dev/null)"
    grafana_ready="$(kubectl get deploy grafana -n mdyaipay-infra -o jsonpath='{.status.readyReplicas}' 2>/dev/null)"
    if [[ "$prom_ready" == "1" && "$grafana_ready" == "1" ]]; then
      if curl -sf "http://127.0.0.1:9090/-/ready" >/dev/null 2>&1 \
        && curl -sf "http://127.0.0.1:3000/api/health" >/dev/null 2>&1; then
        echo "K8s Prometheus + Grafana 已就绪"
        return 0
      fi
    fi
    sleep 2
    i=$((i + 1))
  done
  echo "Prometheus/Grafana 未在预期时间内就绪" >&2
  kubectl get deploy,pod,svc -n mdyaipay-infra -l app.kubernetes.io/name=prometheus 2>&1 || true
  kubectl get deploy,pod,svc -n mdyaipay-infra -l app.kubernetes.io/name=grafana 2>&1 || true
  return 1
}
