# 商户模块设计摘要

完整规格见 [`superpowers/specs/2026-09-17-user-system-design.md`](superpowers/specs/2026-09-17-user-system-design.md) §4.3–4.4。  
分步计划见 [`superpowers/plans/2026-09-17-merchant-module.md`](superpowers/plans/2026-09-17-merchant-module.md)。

## 已实现（Task 1–8）

- **API**：`MerchantFacade`、签名 DTO、`UserErrorCodes`（含中文 Javadoc）
- **domain / service**：`domain.merchant`、`service.merchant` — 状态机、`MerchantApplicationService`、`MerchantSignGuard`
- **dao**：`dao.merchant`（MyBatis 仓储/Mapper/Row）、`dao.outbox`（Outbox）、`dao.support.mybatis`（类型处理器）
- **integration**：`integration.merchant.sign`（验签）、`integration.crypto`（AES）
- **接入**：`controller.merchant`（HTTP）、`api.dubbo.merchant`（Dubbo）
- **Outbox / MQ**：`DefaultMerchantAuditOutboxRecorder` + `OutboxMessageRelay`（`user.mq.enabled=true` 时扫描投递）
- **Dubbo / Boot**：`MerchantFacadeImpl`、`MdyaipayUserApplication`；HTTP `8082`，Dubbo `20882`
- **HTTP（网关可代理）**：`MerchantRestController`，前缀 `/api/v1/merchants`，响应 `ApiResponse`（与 Dubbo 一致）
- **配置**：`application.yml` — `USER_JDBC_*`、`USER_AES_KEY`、`USER_MQ_ENABLED`、`ROCKETMQ_NAMESRV`

### 网关加密收单（`POST /api/v1/payments/collect`）

1. 商户外层 JSON：`appKey`/`app_key`、`timestamp`、`nonce`、`signMethod`、`sign`、`payload`（AES-GCM 密文，密钥 `SHA-256(appSecret)`）。
2. 外层 HMAC 参与字段：`app_key`、`timestamp`、`nonce`、`sign_method=HMAC_SHA256`、`payload`（与开放 API 相同 canonical 规则，见 `MerchantOpenApiSignatures`）。
3. `payload` 明文 JSON：`merchant_id`、`amount`、`channel`、可选 `order_no`/`product_type`。
4. 网关：Dubbo `MerchantGatewayFacade.resolveOpenApiCredential`（JDK 网关走 HTTP `/internal/v1/open-api/credentials/resolve`）→ 验签 → 解密 → 转发 payment 明文 `{ merchantId, orderNo, amount, channel, productType }`。

### HTTP 路径（经 `mdyaipay-gateway` 或直连 8082）

| 方法 | 路径 | 对应 Facade |
|------|------|-------------|
| POST | `/api/v1/merchants` | `createMerchant` |
| POST | `/api/v1/merchants/credentials/issue` | `issueApiCredential` |
| POST | `/api/v1/merchants/audit/submit` | `submitMerchantAudit` |
| POST | `/api/v1/merchants/audit/approve` | `approveMerchant` |
| POST | `/api/v1/merchants/audit/reject` | `rejectMerchant` |
| POST | `/api/v1/merchants/shops` | `createShop` |
| POST | `/api/v1/merchants/shops/operating-status` | `updateShopOperatingStatus` |
| POST | `/api/v1/merchants/members` | `addMember` |
| GET | `/api/v1/merchants/members/assert?merchantId=&userId=&requiredRole=` | `assertMemberRole` |

## 商户验签规则（调用方）

1. 平台 `issueApiCredential` 取得 `appKey`、`appSecret`（secret 只返回一次）。
2. 组装参与签名字段：`app_key`、`timestamp`（毫秒字符串）、`nonce`、`sign_method=HMAC_SHA256`，再加业务字段（如 `merchant_id`、`shop_name`）。
3. 按 key 字典序拼 `k=v&…`，HMAC-SHA256(secret) → 小写 hex，填入 `sign`。
4. 请求体携带 `MerchantSignEnvelope` + 业务字段。

## 商户凭证落库

| 表 | 说明 |
|----|------|
| `merchant_api_credential` | 商户开放 API 凭证：`app_key` 唯一；`secret_cipher` 为 AES-GCM 密文；`status` 为 `ACTIVE`/`DISABLED` |

- **签发**：`POST /api/v1/merchants/credentials/issue` → `MerchantApplicationService#issueApiCredential` → `MerchantApiCredentialRepository#save`（MyBatis `upsert`）。
- **网关解析**：按 `appKey` 查表解密，见 `MerchantOpenApiCredentialService` / `MerchantGatewayInternalController`。
- **压测**：`scripts/seed-loadtest-merchant.py` 走签发 API，库内已有密文；`target/loadtest-merchant-credentials.json` 仅保存**签发时返回一次的明文**供压测构造签名（不可从库中反查明文给客户端）。

启动时 `user.jdbc.init-schema=true` 会执行 `db/schema-merchant-mysql.sql` 与增量 `db/patch-merchant-mysql.sql`（补建凭证表）。

## 启动

```bash
cd mdyaipay-user
mvn -q spring-boot:run
# 生产开启 MQ：USER_MQ_ENABLED=true ROCKETMQ_NAMESRV=host:9876
```
