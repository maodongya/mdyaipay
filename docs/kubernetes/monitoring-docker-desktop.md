# Prometheus + Grafana（Docker Desktop Kubernetes）

归属 **[mdyaipay-infra 基础设施组](mdyaipay-infra-group.md)**。

## 组件

| 名称 | 类型 | 镜像 | 宿主机 |
|------|------|------|--------|
| `prometheus` | Deployment + PVC 10Gi | `prom/prometheus:v2.51.0` | http://127.0.0.1:9090 |
| `grafana` | Deployment + PVC 5Gi | `grafana/grafana:10.4.0` | http://127.0.0.1:3000 |

## 配置来源

Kustomize `kubernetes/infra/monitoring/` 挂载：

- `prometheus/prometheus.yml` — scrape 配置
- `prometheus/rules/` — 与 `docs/monitoring/prometheus/` 同步的副本（改规则后跑 `./scripts/sync-local-monitoring.sh`）
- `grafana/dashboards/` — 看板 JSON 副本

修改规则后：

```bash
./scripts/sync-local-monitoring.sh
# 或
kubectl apply -k kubernetes/infra/monitoring && curl -X POST http://127.0.0.1:9090/-/reload
```

## 部署

```bash
chmod +x scripts/apply-mdyaipay-monitoring-k8s.sh scripts/migrate-monitoring-docker-to-k8s.sh
./scripts/migrate-monitoring-docker-to-k8s.sh   # 停 pay-prometheus / pay-grafana 并部署 K8s
# 或
./scripts/apply-mdyaipay-monitoring-k8s.sh
```

Grafana 管理员：

- 用户 `MDYAIPAY_GRAFANA_ADMIN_USER`（默认 `admin`）
- 密码 `MDYAIPAY_GRAFANA_ADMIN_PASSWORD`（默认 `admin`）

## 前置

业务指标需 **mdyaipay Docker 栈已启动**（8041/8082/8081 等），Prometheus 从 Pod 内访问 `host.docker.internal` 抓取。

## 看板

http://127.0.0.1:3000/d/mdyaipay-ratelimit-gateway

## 回滚到 Docker pay-settle 监控栈

1. `kubectl scale deploy prometheus grafana -n mdyaipay-infra --replicas=0`
2. 在 `ai-pay-settle/docs/docker/monitoring` 执行 `docker compose up -d prometheus grafana`

设计说明：`docs/superpowers/specs/2026-09-21-monitoring-k8s-design.md`
