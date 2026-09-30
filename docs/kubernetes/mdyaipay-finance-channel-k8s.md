# finance 渠道与对账（Kubernetes）

命名空间 **`mdyaipay`**，清单：

- [`kubernetes/apps/finance-mock.yaml`](../../kubernetes/apps/finance-mock.yaml)
- [`kubernetes/apps/finance-gateway.yaml`](../../kubernetes/apps/finance-gateway.yaml)
- [`kubernetes/apps/finance.yaml`](../../kubernetes/apps/finance.yaml)

## 访问（LoadBalancer → localhost）

| 服务 | 节点 1 | 节点 2 | 容器 HTTP | Dubbo |
|------|--------|--------|-----------|-------|
| finance-mock | 8097 | 8098 | 8097 | — |
| finance-gateway | 8087 | 8088 | 8087 | 20887 |
| finance（对账） | 8091 | 8092 | 8090 | 20890 |

## 链路

payment 默认 **`PAYMENT_CHANNEL_SOURCE=dubbo`** → **`FinanceChannelGatewayFacade`**（finance-gateway）→ HTTP **`/internal/channel/*`**（finance-mock）。

对账服务 **`mdyaipay-finance`** 经 Dubbo 调 payment 查成功收单；与渠道 mock 无硬依赖，可与 mock/gateway 并行部署。

## 环境变量要点

| 组件 | 变量 |
|------|------|
| finance-gateway | `FINANCE_GATEWAY_CHANNEL_BASE_URL=http://mdyaipay-finance-mock-1.mdyaipay.svc.cluster.local:8097` |
| finance | `FINANCE_JDBC_*`（Secret `mdyaipay-jdbc` / `FINANCE_JDBC_PASSWORD`） |
| payment | `PAYMENT_CHANNEL_SOURCE=dubbo`（见 `kubernetes/apps/payment.yaml`） |

## 部署与联调

```bash
./scripts/rebuild-mdyaipay-infra-k8s.sh   # 若 infra 未就绪
./scripts/run-mdyaipay-k8s.sh             # 构建镜像 + apply 全栈
./scripts/run-mdyaipay-k8s-smoke.sh       # 造商户 + 网关加密收单冒烟
```

全栈说明见 [mdyaipay-apps-docker-desktop.md](mdyaipay-apps-docker-desktop.md)。
