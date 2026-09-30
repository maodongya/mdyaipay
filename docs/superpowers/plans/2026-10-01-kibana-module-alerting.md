# Kibana Module Alerting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Docker Desktop Kubernetes 中为七个 MDYAI Pay 运行服务建立可部署、可验证、可切换阈值的 Kibana 日志报警。

**Architecture:** 七个服务复用一个资源型 Maven 模块输出 ECS JSON，Filebeat DaemonSet 采集 stdout 并写入单节点 Elasticsearch。Kibana Bootstrap Job 根据规则目录幂等创建 Data View、Server Log/可选 Webhook Connector 和 `.index-threshold` 规则。

**Tech Stack:** Java 17、Spring Boot 3.5.6、Log4j2、Elastic ECS Logging Java、Elastic Stack 8.19.4、Filebeat 8.19.4、Kubernetes/Kustomize、Python 3.12 标准库。

## Global Constraints

- 只覆盖 gateway、user、payment、accounting、finance、finance-gateway、finance-mock 七个运行服务。
- 默认 `demo` profile，同时支持 `production`；规则每分钟执行，统计窗口见设计文档。
- Server Log Connector 必须存在；Webhook 仅在 Secret 提供 URL 时创建。
- 本地 Elastic 关闭认证只用于 Docker Desktop，不得描述成生产安全配置。
- Java 新增或修改的类与方法必须有简体中文 Javadoc；单文件不超过 500 行，单方法有效代码不超过 50 行。
- 不修改用户现有 `.vscode/settings.json`、`.factorypath` 和压测场景改动。

---

### Task 1: 统一 ECS JSON 日志输出

**Files:**
- Create: `mdyaipay-tools/mdyaipay-tools-logging/pom.xml`
- Create: `mdyaipay-tools/mdyaipay-tools-logging/src/main/resources/log4j2-spring.xml`
- Create: `mdyaipay-tools/mdyaipay-tools-logging/src/test/java/com/mdyaipay/tools/logging/LoggingConfigurationTest.java`
- Create: `mdyaipay-tools/mdyaipay-tools-logging/src/test/java/com/mdyaipay/tools/logging/package-info.java`
- Modify: `pom.xml`
- Modify: `mdyaipay-tools/pom.xml`
- Modify: seven runtime service `pom.xml` files
- Delete: local `log4j2-spring.xml` files in gateway、user、payment、accounting、finance

**Interfaces:**
- Produces: classpath resource `log4j2-spring.xml`; Maven artifact `com.mdyaipay:mdyaipay-tools-logging`.
- Consumes: `spring.application.name`、`logging.file.name` 和已有 MDC trace 字段。

- [ ] **Step 1: Write the failing resource test**

```java
/**
 * 验证共享日志配置包含控制台、滚动文件与 ECS 布局。
 */
class LoggingConfigurationTest {
    /** 共享配置必须提供稳定的 ECS 字段与两个输出端。 */
    @Test
    void shouldProvideEcsConsoleAndRollingFile() throws Exception {
        String xml = new String(
                Objects.requireNonNull(getClass().getResourceAsStream("/log4j2-spring.xml")).readAllBytes(),
                StandardCharsets.UTF_8);
        assertTrue(xml.contains("EcsLayout"));
        assertTrue(xml.contains("serviceName"));
        assertTrue(xml.contains("Console"));
        assertTrue(xml.contains("RollingFile"));
    }
}
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-logging -am test`

Expected: FAIL because the module/resource does not exist.

- [ ] **Step 3: Add the shared module and dependency**

Use `co.elastic.logging:log4j2-ecs-layout:1.7.0`. Configure Console and RollingFile with `EcsLayout`, `serviceName="${spring:spring.application.name}"`, daily rotation, 14 retained files, and root level `INFO`. Add the module to `mdyaipay-tools/pom.xml`, manage its version in the root POM, and add the artifact to all seven runtime services.

- [ ] **Step 4: Remove per-service conflicting Log4j2 resources**

Delete the five local `log4j2-spring.xml` files so every runtime service resolves exactly one shared configuration.

- [ ] **Step 5: Run tests**

Run: `mvn -pl mdyaipay-tools/mdyaipay-tools-logging,mdyaipay-gateway,mdyaipay-user,mdyaipay-payment,mdyaipay-accounting,mdyaipay-finance,mdyaipay-finance-gateway,mdyaipay-finance-mock -am test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add pom.xml mdyaipay-tools mdyaipay-gateway mdyaipay-user mdyaipay-payment mdyaipay-accounting mdyaipay-finance mdyaipay-finance-gateway mdyaipay-finance-mock
git commit -m "feat(logging): 统一服务 ECS JSON 日志"
```

### Task 2: 建立数据化报警目录与幂等 Bootstrap

**Files:**
- Create: `kubernetes/infra/elastic/bootstrap/alert-catalog.json`
- Create: `kubernetes/infra/elastic/bootstrap/bootstrap_alerts.py`
- Create: `kubernetes/infra/elastic/bootstrap/test_bootstrap_alerts.py`

**Interfaces:**
- Produces: `load_catalog(path, profile) -> list[dict]`、`build_rule(rule, connector_ids) -> dict`、`KibanaClient.reconcile()`.
- Consumes: `ELASTICSEARCH_URL`、`KIBANA_URL`、`ALERT_PROFILE`、可选 `WEBHOOK_URL`.

- [ ] **Step 1: Write catalog validation tests**

Tests must assert:

```python
def test_profiles_generate_23_unique_rules(self):
    for profile in ("demo", "production"):
        rules = load_catalog(CATALOG, profile)
        self.assertEqual(23, len(rules))
        self.assertEqual(23, len({rule["name"] for rule in rules}))

def test_production_thresholds_are_not_lower(self):
    catalog = read_catalog(CATALOG)
    for rule in catalog["event_rules"]:
        self.assertGreaterEqual(rule["thresholds"]["production"], rule["thresholds"]["demo"])
```

The 23 rules are seven common ERROR rules, one heartbeat rule, and fifteen module event rules.

- [ ] **Step 2: Run tests and confirm failure**

Run: `python3 -m unittest kubernetes/infra/elastic/bootstrap/test_bootstrap_alerts.py -v`

Expected: FAIL because implementation/catalog is absent.

- [ ] **Step 3: Implement catalog expansion**

Store service names once, event rules once, and both thresholds on each catalog entry. Expand common ERROR rules per service. Build `.index-threshold` payloads with deterministic names/tags, `logs-mdyaipay-*`, `@timestamp`, `groupBy=all`, one-minute schedule and Server Log plus optional Webhook actions.

- [ ] **Step 4: Implement API reconciliation**

Use only Python standard library. Reconcile by stable name:

```python
connector_id = client.upsert_connector("mdyaipay-server-log", ".server-log", {}, {})
client.upsert_data_view("mdyaipay-logs", "logs-mdyaipay-*", "@timestamp")
for rule in load_catalog(catalog_path, profile):
    client.upsert_rule(build_rule(rule, connector_ids))
```

Create Elasticsearch ILM and index template before Kibana objects. Wait with bounded retries and exit non-zero on required failures. Skip optional Webhook when URL is blank.

- [ ] **Step 5: Test first-create and repeat-update behavior**

Use an in-process `http.server.ThreadingHTTPServer` fake. Assert a second reconciliation updates existing connector/rules and does not POST duplicates.

- [ ] **Step 6: Run tests and commit**

Run: `python3 -m unittest kubernetes/infra/elastic/bootstrap/test_bootstrap_alerts.py -v`

Expected: PASS.

```bash
git add kubernetes/infra/elastic/bootstrap
git commit -m "feat(kibana): 增加幂等报警规则装配"
```

### Task 3: 部署 Elastic、Filebeat 与日志心跳

**Files:**
- Create: `kubernetes/infra/elastic/kustomization.yaml`
- Create: `kubernetes/infra/elastic/elasticsearch.yaml`
- Create: `kubernetes/infra/elastic/kibana.yaml`
- Create: `kubernetes/infra/elastic/filebeat.yaml`
- Create: `kubernetes/infra/elastic/bootstrap-job.yaml`
- Create: `kubernetes/infra/elastic/secret.yaml.example`
- Create: `kubernetes/apps/log-pipeline-heartbeat.yaml`
- Modify: `kubernetes/infra/kustomization.yaml`
- Modify: `kubernetes/apps/kustomization.yaml`
- Create: `scripts/apply-mdyaipay-elastic-k8s.sh`

**Interfaces:**
- Produces: services `elasticsearch.mdyaipay-infra:9200`、`kibana.mdyaipay-infra:5601`; index pattern `logs-mdyaipay-*`.
- Consumes: Secret `mdyaipay-kibana-secrets` with `ENCRYPTION_KEY` and optional `WEBHOOK_URL`.

- [ ] **Step 1: Write manifests with pinned images**

Use `docker.elastic.co/elasticsearch/elasticsearch:8.19.4`, matching Kibana/Filebeat versions, one ES replica with PVC, readiness probes, and local-only `xpack.security.enabled=false`.

- [ ] **Step 2: Configure Filebeat**

Use a DaemonSet with read-only host mounts for `/var/log/containers` and `/var/log/pods`. Filter namespace `mdyaipay`, decode ECS JSON, map known container names to `service.name`, extract `event`/`outcome`, and route to `logs-mdyaipay-%{[service.name]}-%{+yyyy.MM.dd}`.

- [ ] **Step 3: Add Bootstrap Job and heartbeat**

Generate ConfigMaps from `bootstrap_alerts.py` and `alert-catalog.json`. Mount them into a Python 3.12 Alpine Job. Add a per-minute CronJob that prints one ECS JSON line with `service.name=mdyaipay-log-pipeline` and `event=log_pipeline_heartbeat`.

- [ ] **Step 4: Add safe deployment script**

The script must create/update the encryption Secret without printing it, accept `ALERT_PROFILE`, optionally read `MDYAIPAY_ALERT_WEBHOOK_URL`, apply Elastic Kustomization, and wait for ES/Kibana/Bootstrap readiness.

- [ ] **Step 5: Render and validate manifests**

Run:

```bash
kubectl kustomize kubernetes/infra
kubectl kustomize kubernetes/apps
bash -n scripts/apply-mdyaipay-elastic-k8s.sh
```

Expected: all commands succeed.

- [ ] **Step 6: Commit**

```bash
git add kubernetes/infra kubernetes/apps scripts/apply-mdyaipay-elastic-k8s.sh
git commit -m "feat(k8s): 部署 Elastic 日志报警链路"
```

### Task 4: 增加 gateway 与 user 稳定业务事件

**Files:**
- Modify: `mdyaipay-gateway/src/main/java/com/mdyaipay/gateway/payment/PaymentDubboGatewayFilter.java`
- Create: `mdyaipay-gateway/src/test/java/com/mdyaipay/gateway/payment/PaymentDubboGatewayFilterLoggingTest.java`
- Modify: `mdyaipay-user/src/main/java/com/mdyaipay/user/service/merchant/MerchantSignGuard.java`
- Modify: `mdyaipay-user/src/main/java/com/mdyaipay/user/integration/mq/OutboxMessageRelay.java`
- Create: `mdyaipay-user/src/test/java/com/mdyaipay/user/service/merchant/MerchantSignGuardLoggingTest.java`
- Create: `mdyaipay-user/src/test/java/com/mdyaipay/user/integration/mq/OutboxMessageRelayLoggingTest.java`

**Interfaces:**
- Produces event tokens: `gateway_upstream_failed`、`merchant_auth_rejected`、`outbox_relay_failed`.

- [ ] **Step 1: Add failing log-capture tests**

Use Log4j2 `ListAppender` around the exact logger. Trigger each failure and assert the formatted message contains:

```text
event=gateway_upstream_failed outcome=failure
event=merchant_auth_rejected outcome=failure
event=outbox_relay_failed outcome=failure
```

- [ ] **Step 2: Run focused tests and confirm failure**

Run module-specific test selectors with `mvn -pl <module> -am -Dtest=<Test>,... -Dsurefire.failIfNoSpecifiedTests=false test`.

- [ ] **Step 3: Add minimal event logs**

Log once at the boundary that converts the failure into an HTTP/API response. Preserve exception stack for technical failures; do not log signature payloads, request bodies, secrets, or credentials.

- [ ] **Step 4: Run gateway and user tests**

Run: `mvn -pl mdyaipay-gateway,mdyaipay-user -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add mdyaipay-gateway mdyaipay-user
git commit -m "feat(alerting): 增加网关与用户报警事件"
```

### Task 5: 增加 payment 与 accounting 稳定业务事件

**Files:**
- Modify: `mdyaipay-payment/src/main/java/com/mdyaipay/payment/service/collect/PaymentApplicationService.java`
- Modify: `mdyaipay-payment/src/main/java/com/mdyaipay/payment/service/payout/PayoutApplicationService.java`
- Modify: `mdyaipay-payment/src/main/java/com/mdyaipay/payment/service/withhold/WithholdApplicationService.java`
- Modify: `mdyaipay-payment/src/main/java/com/mdyaipay/payment/integration/accounting/DubboPaymentCollectAccountingNotifier.java`
- Modify: `mdyaipay-accounting/src/main/java/com/mdyaipay/accounting/api/dubbo/wallet/UserWalletFacadeImpl.java`
- Modify: `mdyaipay-accounting/src/main/java/com/mdyaipay/accounting/api/dubbo/wallet/MerchantWalletFacadeImpl.java`
- Modify: `mdyaipay-accounting/src/main/java/com/mdyaipay/accounting/api/dubbo/collect/CollectAccountingFacadeImpl.java`
- Create: `mdyaipay-payment/src/test/java/com/mdyaipay/payment/service/PaymentAlertEventLoggingTest.java`
- Create: `mdyaipay-accounting/src/test/java/com/mdyaipay/accounting/api/dubbo/AccountingAlertEventLoggingTest.java`

**Interfaces:**
- Produces event tokens: `payment_channel_failed`、`payment_failed`、`accounting_notify_failed`、`wallet_entry_failed`、`wallet_version_conflict`.

- [ ] **Step 1: Add failing log-capture tests**

Exercise channel exception, synchronous failed payment, accounting notification exception, unexpected wallet exception, and version conflict. Assert one stable event token and `outcome=failure` per path.

- [ ] **Step 2: Run focused tests and confirm failure**

Run: `mvn -pl mdyaipay-payment,mdyaipay-accounting -am test`

Expected: tests fail only on missing event logs.

- [ ] **Step 3: Implement boundary logging**

Keep payment status behavior unchanged. Log `payment_channel_failed` for thrown channel calls, `payment_failed` for business failure results, and `accounting_notify_failed` where accounting failure is intentionally swallowed. In accounting, distinguish version conflict from other entry failures by error code.

- [ ] **Step 4: Run payment/accounting tests and commit**

Run: `mvn -pl mdyaipay-payment,mdyaipay-accounting -am test`

Expected: PASS.

```bash
git add mdyaipay-payment mdyaipay-accounting
git commit -m "feat(alerting): 增加支付与账务报警事件"
```

### Task 6: 增加 finance、finance-gateway 与 finance-mock 稳定业务事件

**Files:**
- Modify: `mdyaipay-finance/src/main/java/com/mdyaipay/finance/api/dubbo/ReconciliationFacadeImpl.java`
- Modify: `mdyaipay-finance-gateway/src/main/java/com/mdyaipay/financegateway/integration/FinanceChannelHttpClient.java`
- Modify: `mdyaipay-finance-mock/src/main/java/com/mdyaipay/financemock/service/ChannelMockService.java`
- Modify/Create: corresponding focused tests

**Interfaces:**
- Produces event tokens: `reconciliation_failed`、`reconciliation_difference_found`、`channel_http_failed`、`channel_http_timeout`、`mock_channel_failed`.

- [ ] **Step 1: Add failing tests**

Assert reconciliation exceptions and non-empty difference results emit distinct events. Stub HTTP 5xx and timeout separately. Use deterministic mock order numbers that produce a failed mock result and assert its event.

- [ ] **Step 2: Run focused tests and confirm failure**

Run: `mvn -pl mdyaipay-finance,mdyaipay-finance-gateway,mdyaipay-finance-mock -am test`

- [ ] **Step 3: Add minimal event logs**

Emit reconciliation difference count without logging bill rows. Classify `HttpTimeoutException` separately from other HTTP failures. Log mock failures at INFO/WARN for demo observability without changing deterministic outcomes.

- [ ] **Step 4: Run tests and commit**

Run: `mvn -pl mdyaipay-finance,mdyaipay-finance-gateway,mdyaipay-finance-mock -am test`

Expected: PASS.

```bash
git add mdyaipay-finance mdyaipay-finance-gateway mdyaipay-finance-mock
git commit -m "feat(alerting): 增加财务渠道报警事件"
```

### Task 7: 文档、静态审计与端到端验证

**Files:**
- Create: `docs/monitoring/kibana-alerting.md`
- Modify: `README.md`
- Modify: `docs/modules.md`
- Modify: `.gitignore`

**Interfaces:**
- Produces: operator workflow for install, profile switching, webhook Secret, trigger, inspect and uninstall.

- [ ] **Step 1: Write operator documentation**

Document exact commands:

```bash
ALERT_PROFILE=demo ./scripts/apply-mdyaipay-elastic-k8s.sh
kubectl -n mdyaipay-infra port-forward svc/kibana 5601:5601
python3 -m unittest kubernetes/infra/elastic/bootstrap/test_bootstrap_alerts.py -v
```

Include local-only security warning, resource requirements, profile thresholds, optional Webhook environment variable, Kibana rule location, and troubleshooting.

- [ ] **Step 2: Run repository checks**

Run:

```bash
python3 scripts/java-comment-audit.py
python3 -m unittest kubernetes/infra/elastic/bootstrap/test_bootstrap_alerts.py -v
kubectl kustomize kubernetes/infra >/dev/null
kubectl kustomize kubernetes/apps >/dev/null
mvn test
```

Expected: PASS.

- [ ] **Step 3: Deploy and smoke test when Docker Desktop Kubernetes is available**

Run the apply script, wait for Bootstrap completion, verify `logs-mdyaipay-*` has heartbeat documents, list Kibana rules through the API, rerun Bootstrap, and assert rule/connector counts are unchanged. If no cluster is available, record this as an environment limitation without claiming integration success.

- [ ] **Step 4: Check changed-file lint and Git scope**

Confirm no generated `.superpowers/`, credentials, `.factorypath`, `.vscode/settings.json`, log files, or local PVC data enter the commit.

- [ ] **Step 5: Commit**

```bash
git add docs README.md .gitignore
git commit -m "docs: 补充 Kibana 报警运维说明"
```
