# 监控栈（Prometheus + Grafana）迁入 mdyaipay-infra

## 目标

- Deployment `prometheus`、`grafana`，LoadBalancer **9090 / 3000**。
- 规则与看板 **不复制**，由 Kustomize 引用 `docs/monitoring/`。
- 停止 Docker `pay-prometheus`、`pay-grafana`；TSDB/Grafana 库默认新建 PVC（不迁 Docker volume）。

## 非目标

- node-exporter / cadvisor / mysqld-exporter（仍留在 pay-settle compose，可按需后续加入 infra 组）。
