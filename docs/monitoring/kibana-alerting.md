# Kibana 模块报警（Docker Desktop Kubernetes）

归属 **[mdyaipay-infra 基础设施组](../kubernetes/mdyaipay-infra-group.md)**。日志采集只读取 `mdyaipay` 命名空间的容器 stdout。

本方案**不是**生产安全配置：本地关闭 Elasticsearch/Kibana 身份认证，单节点无副本。生产必须启用 TLS、认证、NetworkPolicy 和多副本。

## 组件

| 名称 | 类型 | 镜像 | 宿主机 |
|------|------|------|--------|
| `elasticsearch` | StatefulSet + PVC 10Gi | `elasticsearch:8.19.4` | http://127.0.0.1:9200 |
| `kibana` | Deployment | `kibana:8.19.4` | http://127.0.0.1:5601 |
| `filebeat` | DaemonSet | `filebeat:8.19.4` | 采集 `mdyaipay` Pod 日志 |
| `kibana-alert-bootstrap` | Job | `python:3.12-alpine` | 幂等创建规则 |
| `mdyaipay-log-pipeline-heartbeat` | CronJob | `python:3.12-alpine` | 每分钟心跳 |

索引：`logs-mdyaipay-<service>-yyyy.MM.dd`。Data View：`logs-mdyaipay-*`。

## 部署

```bash
chmod +x scripts/apply-mdyaipay-elastic-k8s.sh
ALERT_PROFILE=demo ./scripts/apply-mdyaipay-elastic-k8s.sh
# 可选 Webhook（钉钉/企业微信中转）
MDYAIPAY_ALERT_WEBHOOK_URL="https://example.local/hooks/mdyaipay" \
  ALERT_PROFILE=demo ./scripts/apply-mdyaipay-elastic-k8s.sh
```

切换 production 阈值会重建 Bootstrap Job，并按名称更新已有规则：

```bash
ALERT_PROFILE=production ./scripts/apply-mdyaipay-elastic-k8s.sh
```

加密密钥存在 Secret `mdyaipay-kibana-secrets` 的 `ENCRYPTION_KEY`，脚本不会打印它。首次部署若 Secret 不存在会自动生成。

## 规则范围

覆盖七个运行服务：gateway、user、payment、accounting、finance、finance-gateway、finance-mock。

每服务一条 `ERROR` 激增规则，再加模块专属 `event.action` 规则，以及日志链路心跳缺失规则。demo 阈值低，便于本地验证；production 阈值更高，仍需按真实流量校准。

Kibana 菜单：`Stack Management → Rules`。默认通知写入 Kibana Server Log；配置 `MDYAIPAY_ALERT_WEBHOOK_URL` 后追加 Webhook。

## 静态校验

```bash
python3 -m unittest kubernetes/infra/elastic/bootstrap/test_bootstrap_alerts.py -v
kubectl kustomize kubernetes/infra/elastic >/dev/null
kubectl kustomize kubernetes/apps >/dev/null
kubectl kustomize kubernetes/infra --load-restrictor LoadRestrictionsNone >/dev/null
```

## 排障

| 现象 | 处理 |
|------|------|
| Bootstrap Job 失败 | `kubectl logs -n mdyaipay-infra job/kibana-alert-bootstrap` |
| 没有日志索引 | 确认业务 Pod 把日志打到 stdout，且 Filebeat 只收 `mdyaipay` 命名空间 |
| 规则不触发 | 先查 Discover 是否有 `event.action` / `log.level: ERROR`，再看 Rule 执行历史 |
| 连接器解密失败 | 不要轮换 `ENCRYPTION_KEY`；必须换密钥时删除连接器后重新 Bootstrap |
| Elasticsearch 起不来 | Docker Desktop 内存建议 ≥ 8Gi；可检查 `vm.max_map_count` |

卸载（保留 PVC 数据）：

```bash
kubectl delete -k kubernetes/infra/elastic
kubectl delete -f kubernetes/apps/log-pipeline-heartbeat.yaml
```
