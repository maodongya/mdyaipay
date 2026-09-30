# finance-gateway / finance-mock（Kubernetes 规划）

| 服务 | 建议端口 | 说明 |
|------|----------|------|
| mdyaipay-finance-mock | 8097 | 模拟银行 HTTP |
| mdyaipay-finance-gateway | 8087 / Dubbo 20887 | payment Dubbo 入口，HTTP 调 mock |

环境变量示例：

- mock：`FINANCE_MOCK_SERVER_PORT=8097`
- gateway：`FINANCE_GATEWAY_CHANNEL_BASE_URL=http://mdyaipay-finance-mock-1.mdyaipay.svc.cluster.local:8097`
- payment：`PAYMENT_CHANNEL_SOURCE=dubbo`

清单可按 `kubernetes/apps/payment.yaml` 模式追加 Deployment（当前仓库以本地 `spring-boot:run` 联调为主）。
