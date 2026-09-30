# Kibana 模块报警设计

## 1. 目标与边界

本设计为 Docker Desktop Kubernetes 中的 MDYAI Pay 运行服务建立可重复部署的日志采集与 Kibana 报警链路。

覆盖以下七个运行服务：

- `mdyaipay-gateway`
- `mdyaipay-user`
- `mdyaipay-payment`
- `mdyaipay-accounting`
- `mdyaipay-finance`
- `mdyaipay-finance-gateway`
- `mdyaipay-finance-mock`

API、Tools 和 Maven 聚合模块不独立运行，因此不虚构独立报警。现有 Prometheus/Grafana 继续负责 CPU、内存、Pod 存活和指标类报警；Kibana 负责日志错误、业务失败和日志采集链路异常。

首版同时提供 `demo` 与 `production` 两套阈值，默认启用 `demo`。通知始终包含 Kibana Server Log，可通过 Kubernetes Secret 追加通用 Webhook；仓库不保存真实 Webhook 地址或凭据。

## 2. 技术方案

采用 Elastic Stack 8.x 固定版本的 K8s 原生方案：

1. 服务通过 Log4j2 向 stdout 输出 ECS JSON 日志。
2. Filebeat DaemonSet 读取 `mdyaipay` 命名空间的容器日志，补充 Kubernetes 元数据并写入 Elasticsearch。
3. Elasticsearch 单节点 StatefulSet 保存 `logs-mdyaipay-*` 日志索引，并通过 ILM 删除过期索引。
4. Kibana Deployment 提供日志检索、连接器和规则执行。
5. Bootstrap Job 调用 Elasticsearch/Kibana API，幂等创建索引模板、Data View、连接器和报警规则。
6. Heartbeat CronJob 每分钟向 stdout 输出一条 ECS JSON 心跳，验证容器日志、Filebeat、Elasticsearch 和 Kibana 查询链路。

本地单节点部署不承诺高可用，也不替代生产环境的 Elastic Operator、TLS、身份认证和多副本规划。

## 3. 组件与职责

### 3.1 共享日志配置

新增一个资源型日志模块，为七个服务提供统一 `log4j2-spring.xml`：

- Console 使用 Elastic ECS JSON Layout。
- RollingFile 保留本地开发日志，使用同一 JSON 格式。
- `service.name` 来自 `spring.application.name`。
- MDC 中现有 `traceId`、`spanId` 等字段随日志输出。
- 异常输出包含异常类型、消息和堆栈。

业务日志使用稳定的 `event=<动作>` 标记。Filebeat 将该标记解析为 `event.action`，避免报警规则依赖整段中文或英文文案。日志不得输出密钥、签名原文、银行卡号和完整请求体。

### 3.2 Filebeat

Filebeat 只采集 `mdyaipay` 命名空间：

- 解析 Kubernetes container 日志外层。
- 解码应用输出的 ECS JSON。
- 应用 JSON 缺失 `service.name` 时，从受控的容器名映射补齐服务名。
- 依据 `service.name` 写入 `logs-mdyaipay-<service>-yyyy.MM.dd`。
- 解析消息中的 `event=<动作>` 与 `outcome=<结果>` 为 `event.action`、`event.outcome`。
- 无法映射服务名或不属于 MDYAI Pay 的事件直接丢弃；JSON 解码失败但可映射服务名的事件保留错误标记。

Filebeat 使用最小只读 RBAC 获取 Pod 与命名空间元数据，不授予修改工作负载的权限。

### 3.3 Elasticsearch 与 Kibana

- Elasticsearch 使用单节点 StatefulSet 和 PVC。
- Kibana 使用 Deployment，并通过固定的 `xpack.encryptedSavedObjects.encryptionKey` Secret 保证连接器重启后仍可解密。
- 本地环境关闭 Elastic 身份认证以降低联调复杂度；文档明确该配置不得直接用于生产。
- ILM 默认保留日志 14 天。
- Data View 固定为 `logs-mdyaipay-*`，时间字段为 `@timestamp`。

### 3.4 Bootstrap

Bootstrap 使用仓库内 Python 脚本和 JSON 规则清单：

- 等待 Elasticsearch、Kibana 就绪。
- 创建或更新 ILM、索引模板和 Data View。
- 按名称查找并创建或更新 Server Log Connector。
- Secret 中存在 Webhook URL 时创建或更新 Webhook Connector。
- 根据 `ALERT_PROFILE=demo|production` 加载对应规则。
- 使用稳定名称与标签实现重复执行不产生重复规则。
- 任一步失败时 Job 失败，避免“部署成功但没有规则”的静默状态。

## 4. 日志事件契约

所有可报警业务事件至少包含：

- `@timestamp`
- `log.level`
- `service.name`
- `event.action`
- `event.outcome`
- `trace.id`（请求链路存在时）
- `error.type` 和异常堆栈（发生异常时）

允许附加非敏感定位字段，如 `channel`、`rule_id`、`http.response.status_code`、`error_code` 和 `difference_count`。订单号、商户号等业务标识只能用于定位，不作为通知正文的默认内容。

首版事件动作：

- 网关：`gateway_upstream_failed`、`rate_limit_denied`、`rate_limit_backend_error`
- 用户：`merchant_auth_rejected`、`outbox_relay_failed`
- 支付：`payment_channel_failed`、`payment_failed`、`accounting_notify_failed`
- 账务：`wallet_entry_failed`、`wallet_version_conflict`
- 财务：`reconciliation_failed`、`reconciliation_difference_found`
- 渠道网关：`channel_http_failed`、`channel_http_timeout`
- 渠道 Mock：`mock_channel_failed`
- 日志链路：`log_pipeline_heartbeat`

## 5. 报警目录与阈值

所有规则每分钟执行一次。计数均按最近五分钟窗口计算。

### 5.1 通用规则

每个运行服务各一条 `ERROR` 激增规则：

- demo：至少 1 条。
- production：至少 5 条。
- 严重级别：P1。

日志链路心跳缺失：

- demo：最近 3 分钟少于 1 条。
- production：最近 5 分钟少于 1 条。
- 严重级别：P1。

### 5.2 模块专属规则

| 模块 | 事件 | demo | production | 级别 |
|---|---|---:|---:|---|
| gateway | `gateway_upstream_failed` | 1 | 5 | P1 |
| gateway | `rate_limit_backend_error` | 1 | 3 | P1 |
| gateway | `rate_limit_denied` | 3 | 100 | P2 |
| user | `merchant_auth_rejected` | 3 | 50 | P2 |
| user | `outbox_relay_failed` | 1 | 3 | P1 |
| payment | `payment_channel_failed` | 1 | 5 | P1 |
| payment | `payment_failed` | 2 | 20 | P2 |
| payment | `accounting_notify_failed` | 1 | 3 | P1 |
| accounting | `wallet_entry_failed` | 1 | 5 | P1 |
| accounting | `wallet_version_conflict` | 2 | 20 | P2 |
| finance | `reconciliation_failed` | 1 | 2 | P1 |
| finance | `reconciliation_difference_found` | 1 | 1 | P1 |
| finance-gateway | `channel_http_failed` | 1 | 5 | P1 |
| finance-gateway | `channel_http_timeout` | 1 | 3 | P1 |
| finance-mock | `mock_channel_failed` | 2 | 20 | P2 |

生产阈值是可运行基线，不代表未经真实流量校准即可直接用于生产。规则清单保持数据化，阈值调整不需要修改 Bootstrap 逻辑。

## 6. 通知策略

每次触发发送一条摘要，包含规则名、严重级别、服务名、窗口、命中数和 Kibana 查询入口。默认不展开原始业务日志，避免敏感信息进入群聊。

- Server Log Connector 始终创建，用于本地验证。
- Webhook URL 非空时追加 Webhook Action。
- Webhook Secret 不存在时不失败，只记录“跳过可选 Webhook”。
- 同一规则在活动期间按规则执行周期抑制重复新告警；恢复后允许再次触发。

## 7. 错误处理与安全

- Elasticsearch 或 Kibana 未就绪：Bootstrap 有限次重试，超时后 Job 失败。
- 规则 JSON 无效、名称重复或 profile 不存在：脚本启动即失败。
- Webhook 调用失败：由 Kibana 记录 action execution error，不影响日志入库。
- JSON 解码失败：Filebeat 保留错误标记并发送到统一索引，便于检索格式问题。
- Kibana 加密密钥和 Webhook URL 只从 Secret 注入。
- 本地关闭认证的 Service 仅在 Docker Desktop 使用；生产部署必须启用 TLS、认证和 NetworkPolicy。

## 8. 部署结构

新增目录按现有 Kubernetes 分层：

- `kubernetes/infra/elastic/`：Elasticsearch、Kibana、Filebeat、RBAC、PVC、ConfigMap 和 Kustomization。
- `kubernetes/apps/`：日志链路 Heartbeat CronJob。
- `kubernetes/infra/elastic/bootstrap/`：Bootstrap、规则目录与单元测试；由同目录 Kustomization 直接生成 ConfigMap。
- `scripts/`：部署、规则 profile 切换和冒烟验证入口。
- `docs/monitoring/`：部署、切换 profile、配置 Webhook、触发测试和排障说明。

`kubernetes/infra/kustomization.yaml` 纳入 Elastic 子目录；应用 Kustomization 纳入 Heartbeat。

## 9. 验证策略

### 9.1 静态验证

- Python 单元测试校验规则名称、事件、阈值、严重级别和唯一性。
- Python HTTP 假服务测试 Bootstrap 首次创建与重复更新行为。
- `kubectl kustomize` 验证清单可构建。
- Maven 测试验证新增业务事件在失败路径输出。

### 9.2 集成验证

1. 部署 Elastic Stack 与 Filebeat。
2. 等待 Bootstrap Job 成功。
3. 确认七个服务日志索引和 Data View 可见。
4. 确认 Heartbeat 文档持续进入 Elasticsearch。
5. 触发至少一个 demo 业务事件。
6. 查询 Kibana Rules API，确认规则进入 active 状态。
7. 查询 Kibana 日志，确认 Server Log Connector 执行动作。
8. 再次执行 Bootstrap，确认连接器和规则数量不增加。

## 10. 完成标准

- 七个运行服务均输出统一 ECS JSON。
- Filebeat 能采集并按服务索引日志。
- demo/production 两套规则均通过静态校验。
- 默认 demo 规则可由 Bootstrap 幂等创建。
- Server Log 通知可验证，Webhook 可通过 Secret 启用。
- Heartbeat 能覆盖应用 stdout 到 Kibana 查询的完整日志链路。
- 新增配置、脚本和 Java 改动均有相应测试与中文文档。
