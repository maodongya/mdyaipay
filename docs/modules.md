# 模块拆分说明

`mdyaipay` 采用 Maven 多模块聚合工程，父工程 `artifactId` 为 `mdyaipay`（`packaging=pom`），子模块职责边界如下。

编码与中文注释为 **强制规约**，见 [`方便人理解代码规约.md`](方便人理解代码规约.md) 与 [`.cursor/rules/java-chinese-readable-docs.mdc`](../.cursor/rules/java-chinese-readable-docs.mdc)。设计拆分见 [`设计原则规约.md`](设计原则规约.md) 与 [`.cursor/rules/design-principles.mdc`](../.cursor/rules/design-principles.mdc)。

| 模块 | ArtifactId | 职责 |
|------|--------------|------|
| 公共（聚合） | `mdyaipay-tools` | Maven 聚合：common、timetrace、trace-core、loadtest 等 |
| 公共 | `mdyaipay-tools-common` | 雪花 ID、金额分/元工具、统一 API 响应与错误码（无业务编排） |
| 公共 | `mdyaipay-tools-timetrace` | Spring AOP `@TimeTrace`：入口方法耗时与慢调用报告（无业务编排） |
| 公共 | `mdyaipay-tools-trace-core` | 分布式 Trace 上下文与 W3C/sw8 传播（仅 JDK，无 Spring） |
| 公共 | `mdyaipay-tools-trace-spring-boot` | Trace 自动配置：Servlet/Gateway/Dubbo Filter、MDC、出站 HTTP 头 |
| 公共（聚合） | `mdyaipay-tools-loadtest` | 压测工具聚合：core + HTTP/Dubbo/Spring Cloud 驱动 + CLI |
| 公共 | `mdyaipay-tools-loadtest-core` | 压测引擎、动态指标、报告模型 |
| 公共 | `mdyaipay-tools-loadtest-http` | HTTP/HTTPS 压测驱动 |
| 公共 | `mdyaipay-tools-loadtest-dubbo` | Dubbo 泛化调用压测驱动 |
| 公共 | `mdyaipay-tools-loadtest-springcloud` | Spring Cloud（LB/Feign）压测驱动 |
| 公共 | `mdyaipay-tools-loadtest-cli` | 场景文件驱动的命令行压测入口 |
| 用户 API | `mdyaipay-user-api` | Dubbo Facade 与 DTO（无实现依赖） |
| 支付 API | `mdyaipay-payment-api` | 网关内网 Dubbo Facade（收单/代扣/代付） |
| 用户 | `mdyaipay-user` | 会员/商户主体、认证会话、权限模型；商户开放 API 验签 |
| 账务 | `mdyaipay-accounting` | 账户分录、余额、冻结与入账一致性 |
| 财务 | `mdyaipay-finance` | 结算、对账、差异处理与报表口径 |
| 支付 | `mdyaipay-payment` | 收单（快捷/网银）、代扣、代付等领域模型、应用编排与渠道网关抽象 |
| 网关 | `mdyaipay-gateway` | 对外接入：路由、鉴权、限流（流量「网管」） |
| 收银台 | `mdyaipay-cashier` | 收银台会话、支付方式选择与调用支付核心 |

## 依赖关系（规划）

典型调用链：`gateway` → `cashier` → `payment`；支付成功后异步或同步触发 `accounting` 入账，日终/批次由 `finance` 对账结算。`mdyaipay-tools-common` / `mdyaipay-tools-timetrace` 为无上游业务依赖的公共库，其他模块按需引入。当前代码仅为骨架与支付示例实现，模块间 Maven 依赖可按演进逐步引入，避免过早耦合。

## 构建

在仓库根目录 `javaproject/mdyaipay` 执行：

```bash
mvn clean install
```

仅构建支付 demo：

```bash
cd mdyaipay-payment
mvn -q exec:java
```

启动 HTTP 网关：

```bash
cd mdyaipay-gateway
mvn -q exec:java
```
