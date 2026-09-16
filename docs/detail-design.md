# mdyaipay 详细设计

## 1. 包结构设计（支付模块 mdyaipay-payment）

```text
com.mdyaipay.payment
├── MdyaipayPaymentApplication         # Spring Boot 入口（默认 8081）
├── controller                         # REST + dto
├── service                            # 应用服务（collect | withhold | payout）
├── domain                             # 聚合、仓储接口、渠道端口
├── repository                         # MyBatis 仓储实现、建表初始化
├── mybatis                            # Mapper 接口、Row、TypeHandler；XML 在 resources/mapper
├── gateway                            # 渠道 Mock（实现 domain.*Gateway）
└── config                             # MyBatis 扫描、Mock 渠道 Bean、可选 init-schema
```

配置：`application.yml`（`spring.datasource` + `mybatis.*`）；`payment.jdbc.init-schema=true` 时执行 `db/schema-mysql.sql`。

各能力子包内类型命名与原先扁平 `domain` 一致，例如 `collect` 含 `PaymentOrder`、`PaymentGateway`、`PaymentSubmitResult` 等；`withhold` / `payout` 同理。

其他子模块当前以 `package-info` 占位，包根分别为：`com.mdyaipay.user`、`com.mdyaipay.accounting`、`com.mdyaipay.finance`、`com.mdyaipay.gateway`、`com.mdyaipay.cashier`。

## 2. 领域模型

### 2.1 PaymentOrder（收单）

- 字段：`orderNo`、`amount`、`channel`、`productType`（`PaymentProductType`）、`status`、`createdAt`、`updatedAt`
- `productType`：
  - `QUICK_COLLECTION`：快捷、条码等偏同步确认的收单。
  - `ONLINE_BANKING`：网银跳转类收单；生产环境多在 `PROCESSING` 后依赖回调/查单到达终态。
- 关键行为：`markProcessing()`、`markSuccess()`、`markFailed()`
- 约束：非法状态迁移拒绝（终态不可再变）。

扩展字段（生产建议落地为值对象或扩展表，演示代码未绑定）：

- 网银：`bankCode`、`payerClientType`、`frontReturnUrl`、`notifyUrl`、渠道订单号。

### 2.2 PaymentStatus

- 枚举值：`CREATED`、`PROCESSING`、`SUCCESS`、`FAILED`、`CLOSED`
- 演示代码仍以同步 Mock 为主；生产中网银可在 `PROCESSING` 与终态之间增加「待渠道确认」状态（演进时扩展枚举或子状态机）。

### 2.3 WithholdOrder（代扣）

- 字段：`deductionNo`（商户侧幂等键）、`agreementNo`（签约协议号）、`amount`、`channel`、`status`、时间戳
- 关键行为：与收单类似的状态迁移。
- 前置条件：`agreementNo` 对应协议有效且授权范围覆盖本次扣款（由用户域或签约服务校验）。

### 2.4 WithholdStatus

- `CREATED`、`PROCESSING`、`SUCCESS`、`FAILED`（代扣一般不暴露给 C 端收银台，无需 `CLOSED` 亦可演进补充）。

### 2.5 PayoutOrder（代付）

- 字段：`payoutNo`（商户侧幂等键）、`amount`、`channel`、`payeeRef`（收款方令牌/内部户标识，禁止明文账号）、`status`、时间戳
- 风控与头寸校验建议在应用层编排（调用账务冻结额度后再 `remit`）。

### 2.6 PayoutStatus

- `CREATED`、`PROCESSING`、`SUCCESS`、`FAILED`

### 2.7 网关接口边界

| 接口 | 职责 |
|------|------|
| `PaymentGateway.pay` → `PaymentSubmitResult` | 收单受理：快捷同步终态；网银返回 `AWAITING_CHANNEL_CONFIRMATION`，订单保持 `PROCESSING` |
| `WithholdGateway.deduct` | 单笔代扣提交 |
| `PayoutGateway.remit` | 单笔代付提交 |

渠道回调入口归属 **网关模块**，验签后调用 `PaymentApplicationService.confirmChannelPayment(orderNo, success)`。

### 2.8 HTTP 网关（mdyaipay-gateway）

进程入口：`com.mdyaipay.gateway.MdyaipayGatewayServer`，默认端口 `8090`（环境变量 `MDYAIPAY_GATEWAY_PORT`）。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 存活探测 |
| POST | `/api/v1/payments/collect` | JSON：`orderNo`,`amount`,`channel`[,`productType`] |
| GET | `/api/v1/payments/{orderNo}` | 查询收单 |
| POST | `/api/v1/payments/{orderNo}/channel-confirm` | JSON：`success`（网银异步确认） |
| POST | `/api/v1/withholds` | JSON：`deductionNo`,`agreementNo`,`amount`,`channel` |
| POST | `/api/v1/payouts` | JSON：`payoutNo`,`amount`,`channel`,`payeeRef` |

## 3. 应用服务设计

### 3.1 PaymentApplicationService

- `createAndPay(orderNo, amount, channel)`：等价于 `QUICK_COLLECTION`。
- `createAndPay(..., PaymentProductType)`：仅允许 `QUICK_COLLECTION` / `ONLINE_BANKING`；其他类型须走专用服务，避免错误路由。
- `confirmChannelPayment(orderNo, success)`：网银异步确认，仅 `ONLINE_BANKING` 且 `PROCESSING`。
- `getPayment(orderNo)`：查询订单。

步骤：幂等校验 → 创建订单 → `PROCESSING` → `PaymentGateway.pay` → 若 `SYNC_*` 则落终态；若 `AWAITING_CHANNEL_CONFIRMATION` 则保持 `PROCESSING`。

### 3.2 WithholdApplicationService.createAndDeduct()

- 幂等键：`deductionNo`
- 输入：`deductionNo`、`agreementNo`、`amount`、`channel`
- 步骤：创建 `WithholdOrder` → `PROCESSING` → `WithholdGateway.deduct` → 终态持久化

### 3.3 PayoutApplicationService.createAndRemit()

- 幂等键：`payoutNo`
- 输入：`payoutNo`、`amount`、`channel`、`payeeRef`
- 步骤：创建 `PayoutOrder` → `PROCESSING` → `PayoutGateway.remit` → 终态持久化

异常策略（三类服务一致）：网关运行时异常 → 当前实现记 `FAILED`；生产可区分为「未知」状态并走查单。

## 4. 基础设施设计

### 4.1 仓储（MyBatis + JDBC）

| 实现 | 包路径 | 用途 |
|------|--------|------|
| `MyBatis*OrderRepository` + `*Mapper` | `infrastructure.persistence.mysql` | 生产持久化（MyBatis XML） |

装配：Spring 组件扫描注册 `@Service`、 `@Repository`、MyBatis `@Mapper`。

| 键 / 环境变量 | 说明 |
|----|------|
| `spring.datasource.*` / `PAYMENT_JDBC_*` | 数据源 |
| `payment.jdbc.init-schema` / `PAYMENT_JDBC_INIT_SCHEMA` | `true` 时启动执行 `classpath:db/schema-mysql.sql` |

建表脚本：`mdyaipay-payment/src/main/resources/db/schema-mysql.sql`（表 `payment_order`、`withhold_order`、`payout_order`）。

MyBatis：`src/main/resources/mapper/*.xml`；`PaymentMyBatisConfiguration`（`@MapperScan`）。单测使用 H2（`src/test/resources/application.properties`）。非 Spring 工厂路径通过 `PaymentMyBatisSupport` 构建 `SqlSessionFactory`。

JDK 网关 `MdyaipayGatewayServer` 将支付 API 转发至 payment 服务（`PAYMENT_BASE_URL`，默认 `http://127.0.0.1:8081`）。

### 4.2 Mock 网关

- `MockPaymentGateway`、`MockWithholdGateway`、`MockPayoutGateway`：基于单号哈希模拟成功率，便于流水线无外部依赖运行。

## 5. 时序说明

### 5.1 收单（快捷）

与原版一致：`PaymentApplicationService` → 仓储 → `PaymentGateway.pay`。

### 5.2 网银（逻辑时序）

1. 收银台 → `PaymentApplicationService.createAndPay(..., ONLINE_BANKING)`
2. → `PaymentGateway.pay`：返回跳转信息（演示未序列化，生产为 DTO）
3. 用户在银行完成支付
4. 渠道 → 网关回调 →（规划）领域确认 → `SUCCESS` / `FAILED`
5. 补偿查单定时任务对齐悬空单

### 5.3 代扣

`WithholdApplicationService` → `WithholdOrderRepository` → `WithholdGateway.deduct` → 持久化；异步通知路径同网银。

### 5.4 代付

`PayoutApplicationService` → `PayoutOrderRepository` → `PayoutGateway.remit` → 持久化。

## 6. 测试设计

- 单元测试：
  - 收单成功、网银产品类型、金额非法、幂等
  - 代扣成功与幂等
  - 代付成功与幂等
- 集成测试（后续）：
  - 回调验签与幂等键（渠道单号 / 商户单号）
  - 渠道超时、Unknown 与查单修复
