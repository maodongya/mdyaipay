# 商户模块分步实现计划（含签名 / 验签）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or executing-plans. Steps use checkbox syntax.

**Goal:** 在 `mdyaipay-user-api` + `mdyaipay-user` 内落地商户域（审核、店铺、成员），商户侧开放接口一律 **带签名校验**，密钥可配置、可落库。

**Architecture:** 三层 `service` → `domain` → `dao`；签名为横切能力：`MerchantSignEnvelope` + `MerchantSignatureVerifier`（HMAC-SHA256 规范串）；密钥表 `merchant_api_credential`（secret AES 存储）；平台审核接口不走商户签名。

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis, MySQL/H2, Dubbo 3.3.2（Provider 在后续 Task 挂载）, `mdyaipay-tools-common`

## Global Constraints

- 金额单位：分（本模块暂无金额字段）
- 依赖方向：consumer 仅依赖 `mdyaipay-user-api`
- 商户发起 API：必须携带 `appKey`、`timestampMillis`、`nonce`、`signMethod`、`sign`
- 签名算法默认：`HMAC_SHA256`，canonical 为参与签名字段按 key 字典序 `k=v` 用 `&` 连接
- 时钟偏移：默认 ±300s，配置项 `user.merchant.sign.max-skew-seconds`
- AES 密钥：环境变量 `USER_AES_KEY`（Base64，32 字节），用于 secret 落库加解密

---

### Task 1: API 模块与签名契约

**Files:**
- Create: `mdyaipay-user-api/pom.xml`, `MerchantFacade.java`, `MerchantSignEnvelope.java`, `UserErrorCodes.java`, 各 Command/Result DTO
- Modify: 根 `pom.xml` 增加 module + dependencyManagement

**Produces:** `MerchantSignEnvelope`, `MerchantFacade` 接口方法签名

- [ ] 新增 `mdyaipay-user-api` 模块
- [ ] 定义 `UserErrorCodes.SIGN_INVALID`, `SIGN_EXPIRED`, `CREDENTIAL_DISABLED`, `MERCHANT_STATE_INVALID`
- [ ] 商户侧方法参数包含 `MerchantSignEnvelope` + 业务 Command

---

### Task 2: 签名 / 验签核心（无 Spring）

**Files:**
- Create: `integration/sign/MerchantSignatureSupport.java`, `MerchantSignatureVerifier.java`
- Test: `MerchantSignatureVerifierTest.java`

**Interfaces:**
- Consumes: canonical 规则、HMAC-SHA256
- Produces: `MerchantSignatureVerifier.verify(appKey, envelope, businessParams) → merchantId`

- [ ] 写失败用例：错签、过期、nonce 缺失
- [ ] 实现 `MerchantSignatureSupport.sign` / `verify`
- [ ] `mvn -pl mdyaipay-user test`

---

### Task 3: 领域模型与状态机

**Files:**
- Create: `domain/merchant/Merchant.java`, enums, `Shop`, `MerchantMember`, `MerchantAuditRecord`
- Test: `MerchantTest.java`

- [ ] 审核状态迁移测试
- [ ] 实现聚合方法 `submitAudit`, `approve`, `reject`, `enable`, `disable`

---

### Task 4: 密钥与配置

**Files:**
- Create: `domain/merchant/MerchantApiCredential.java`, `MerchantApiCredentialRepository`, `UserProperties`（sign + crypto）
- Create: `integration/crypto/AesSecretCipher.java`
- Test: `AesSecretCipherTest.java`

- [ ] `UserProperties` 绑定 `user.merchant.sign.*`, `user.crypto.aes-key`
- [ ] DAO 接口 `findByAppKey`, `save`

---

### Task 5: MySQL 表与 MyBatis

**Files:**
- Create: `resources/db/schema-merchant-mysql.sql`（或合并 schema-mysql.sql）
- Create: Mapper/XML, `MyBatis*Repository`, `UserSchemaInitializer`

- [ ] 表：`merchant`, `shop`, `merchant_audit_record`, `merchant_member`, `merchant_api_credential`, `domain_outbox`
- [ ] 与 payment 相同 init-schema 开关模式

---

### Task 6: MerchantApplicationService

**Files:**
- Create: `service/merchant/MerchantApplicationService.java`, `MerchantSignGuard.java`
- Test: `MerchantApplicationServiceTest.java`（Map 仓储）

**Flow:** 商户 API → `MerchantSignGuard.verifyAndResolveMerchantId` → 领域编排

- [ ] `submitMerchantAudit`, `createShop`, `updateShopOperatingStatus`, `addMember` 验签
- [ ] `approveMerchant` / `rejectMerchant` 平台路径不验签（auditor 参数）
- [ ] `issueApiCredential` 平台签发 appKey/secret（返回 secret 仅一次）

---

### Task 7: Outbox + RocketMQ（商户审核事件）

**Files:**
- Create: `integration/mq/OutboxPublisher`, `MerchantAuditEventPayload`
- Test: outbox 写入单测

- [ ] 审核通过/拒绝写 `domain_outbox`
- [ ] 可选 `@Profile("integration")` 真 MQ

---

### Task 8: Dubbo Provider + Boot

**Files:**
- Create: `MdyaipayUserApplication`, `api/dubbo/MerchantFacadeImpl`, `application.yml`, Dubbo config

- [ ] 端口 8082 / Dubbo 20882
- [ ] 健康检查

---

### Task 9: 文档

- Modify: `docs/superpowers/specs/2026-09-17-user-system-design.md` §商户签名
- Modify: `docs/modules.md`, 新增 `docs/user-merchant-design.md` 摘要
