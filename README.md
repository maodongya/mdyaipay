# mdyaipay

`mdyaipay` 是一个面向中小型业务的支付平台示例工程（Maven 多模块），目标是边界清晰的领域拆分、可扩展的渠道接入，以及可审计的支付状态流转。

## 模块结构

| 目录 | 说明 |
|------|------|
| `mdyaipay-tools` | 公共工具聚合（子模块见下） |
| `mdyaipay-tools/mdyaipay-tools-common` | 公共基础聚合（子模块 `mdyaipay-tools-common-core`） |
| `mdyaipay-tools/mdyaipay-tools-timetrace` | 方法耗时追踪聚合（子模块 `mdyaipay-tools-timetrace-core`） |
| `mdyaipay-user` | 用户域 |
| `mdyaipay-accounting` | 账务域 |
| `mdyaipay-finance` | 财务域 |
| `mdyaipay-payment` | 支付核心：收单（快捷/网银）、代扣、代付示例与网关抽象 |
| `mdyaipay-gateway` | 接入网关（路由、鉴权、限流） |
| `mdyaipay-cashier` | 收银台 |
| `mdyaipay-tools/.../ratelimit` | 限流工具（core / redis / spring-boot） |

详见 [`docs/modules.md`](docs/modules.md)。

设计文档：

- [`docs/overview-design.md`](docs/overview-design.md)：概要设计
- [`docs/detail-design.md`](docs/detail-design.md)：详细设计（含网银、代扣、代付分型与包结构）
- [`docs/superpowers/specs/2026-09-20-ratelimit-design.md`](docs/superpowers/specs/2026-09-20-ratelimit-design.md)：限流设计（网关令牌桶 / 服务滑动窗口）
- [`docs/方便人理解代码规约.md`](docs/方便人理解代码规约.md)：可读性规约（§5.0 类/方法中文 Javadoc、单文件 ≤500 行、单方法 ≤50 行有效代码；命名、分层、功能块）
- [`docs/设计原则规约.md`](docs/设计原则规约.md)：SOLID / KISS / DRY / SOC / YAGNI
- [`docs/设计模式规约.md`](docs/设计模式规约.md)：GoF 23 种模式在本工程中的选用与落点

## 构建与运行

在 `javaproject/mdyaipay` 目录：

```bash
mvn clean test
```

运行支付服务（Spring Boot，默认 **8081**）：

```bash
cd mdyaipay-payment
mvn -q spring-boot:run
```

MySQL：配置 `PAYMENT_JDBC_*` 或 `application.yml` 中的 `spring.datasource`，然后 `mvn -q spring-boot:run`。

主类：`com.mdyaipay.payment.MdyaipayPaymentApplication`。

启动对外 HTTP 网关（依赖 `jdk.httpserver` 模块与 Jackson）：

```bash
cd mdyaipay-gateway
mvn -q exec:java
```

默认端口 `8041`（可用 `MDYAIPAY_GATEWAY_PORT` 修改）。接口列表见 `docs/detail-design.md` 第 2.8 节。

网关限流（`mdyaipay.ratelimit.*`）：默认 **Redisson** 滑动窗口日志（`redis://127.0.0.1:6379`），收单 / 代扣 / 代付各 **20/s**（`collect`、`withholds`、`payouts`）。可用 `MDYAIPAY_RATELIMIT_BACKEND=redisson|redis|memory` 覆盖；**backend 必须与 classpath 驱动一致**（改代码后需 `mvn -pl mdyaipay-gateway -am install`）。启动日志应出现 `mdyaipay.ratelimit 已启用 ... gatewayFilter=registered`；压测时 429 需并发突发（>100/s）才明显。设计见 [`docs/superpowers/specs/2026-09-20-ratelimit-design.md`](docs/superpowers/specs/2026-09-20-ratelimit-design.md)。
