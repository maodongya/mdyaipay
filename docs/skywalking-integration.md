# SkyWalking 接入方案（mdyaipay）

## 1. 目标与范围

| 目标 | 说明 |
|------|------|
| 分布式链路 | 网关 HTTP → user/payment（Dubbo Triple 或内网 HTTP）→ MyBatis/JDBC |
| 拓扑与依赖 | 自动发现 `mdyaipay-gateway`、`mdyaipay-user`、`mdyaipay-payment` 调用关系 |
| 性能分析 | 慢 SQL、Dubbo/HTTP 耗时、Gateway Filter 链 |
| 本地可复现 | Docker 起 OAP/UI，Java Agent 挂本地 Spring Boot 进程 |

**本期不做（可二期）**：Log 与 Trace 关联（SW-LTK）、MQ/RocketMQ 插件、K8s 侧车注入、告警规则下发。

**与现有 TimeTrace 的关系**：`mdyaipay-tools-timetrace` 保留为轻量方法级耗时日志；SkyWalking 作为统一 APM。本地可同时开，生产建议以 SkyWalking 为主、TimeTrace 默认关闭或仅开发环境开启。

## 2. 架构

```text
商户/压测客户端
       │ HTTPS
       ▼
mdyaipay-gateway:8041  ──HTTP──► user:8082（验签凭证）
       │                          payment:8081（collect 内网 HTTP，可选）
       │ Dubbo Triple
       ▼
mdyaipay-payment:20881 / mdyaipay-user:20882
       │ JDBC
       ▼
     MySQL

各 Java 进程：-javaagent:skywalking-agent.jar ──gRPC──► OAP:11800 ──► BanyanDB
                                                              └── UI :8090
```

## 3. 接入方式选型

| 方案 | 优点 | 缺点 | 建议 |
|------|------|------|------|
| **A. Java Agent（推荐）** | 零改业务代码；Dubbo/Spring MVC/JDBC/MySQL 插件成熟 | 需 JVM 参数；版本需与 OAP 对齐 | **本地 + 生产首选** |
| B. Micrometer + OTel 导出 | 与 Spring Boot 3 指标统一 | Dubbo 链路需额外配置；不如 Agent 完整 | 仅当禁止 `-javaagent` 时 |
| C. 自研 Filter 上报 | 完全可控 | 成本高、易漏 Span | 不推荐 |

**结论**：采用 **方案 A**，按服务挂 Agent，通过环境变量区分 `SW_AGENT_NAME`。

## 4. 本地 Docker 部署

仓库已提供：

| 路径 | 作用 |
|------|------|
| `docker/skywalking/docker-compose.yml` | BanyanDB + OAP + UI |
| `docker/skywalking/.env.example` | 镜像版本 pin |
| `scripts/run-skywalking.sh` | 一键 `docker compose up` 并等待 OAP |
| `scripts/download-skywalking-agent.sh` | 下载与 OAP 同版本的 Java Agent 到 `target/skywalking-agent/` |
| `scripts/run-with-skywalking-agent.sh` | 带 Agent 启动单个模块 |
| `scripts/run-mdyaipay-services-with-skywalking.sh` | **user + payment + gateway** 三服务带 Agent |
| `scripts/skywalking-agent-env.sh` | Agent JVM 参数（供其它脚本 source） |
| `docker/skywalking/mdyaipay-local.agent.config` | 本地采样/SQL 参数等覆盖 |

### 4.1 启动步骤

```bash
# 1. 起 OAP / UI
./scripts/run-skywalking.sh

# 2. 下载 Agent（默认 9.7.0，与 OAP 10.2 协议兼容；见 SKYWALKING_AGENT_VERSION）
./scripts/download-skywalking-agent.sh

# 3. 带 Agent 启动三服务（推荐）
./scripts/run-mdyaipay-services-with-skywalking.sh
# 可选：同时起 OAP
# SKYWALKING_START_OAP=1 ./scripts/run-mdyaipay-services-with-skywalking.sh

# 或单模块
./scripts/run-with-skywalking-agent.sh mdyaipay-payment

# 压测时挂 Agent（OAP 已起且 Agent 已下载时，run-payment-loadtest 默认 auto 启用）
# ./scripts/run-payment-loadtest.sh
# 强制关闭：ENABLE_SKYWALKING=0 ./scripts/run-payment-loadtest.sh
# 顺带起 OAP：SKYWALKING_START_OAP=1 ./scripts/run-payment-loadtest.sh
```

### 4.2 访问地址

| 组件 | 地址 |
|------|------|
| SkyWalking UI | http://127.0.0.1:8090 |
| OAP HTTP 查询 | http://127.0.0.1:12800 |
| Agent 上报 gRPC | 127.0.0.1:11800 |
| BanyanDB 调试 UI | http://127.0.0.1:17913 |

UI 映射为 **8090**，避免与本项目 payment actuator **8081**、Horizon 默认 **8080** 冲突。

### 4.3 停止与清理

```bash
cd docker/skywalking && docker compose down
# 需清空存储时：docker compose down -v  （当前 compose 未挂持久卷，down 即丢数据）
```

## 5. 各服务 Agent 配置

### 5.1 通用 JVM 参数

```bash
export SW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800
export SW_AGENT_NAME=mdyaipay-payment::local   # 建议：服务名::环境

JAVA_OPTS="-javaagent:/path/to/skywalking-agent.jar"
# 注意：SW_AGENT_* 须为环境变量；写成 -DSW_AGENT_NAME 不会生效
```

`target/skywalking-agent/` 已在 `.gitignore` 的 `target/` 规则内，Agent 不入库。

### 5.2 服务命名建议

| 进程 | `SW_AGENT_NAME` 示例 |
|------|----------------------|
| gateway | `mdyaipay-gateway::local` |
| user | `mdyaipay-user::local` |
| payment | `mdyaipay-payment::local` |

生产使用 `::prod`、`::staging`，便于 UI 按环境过滤。

### 5.3 可选 Agent 配置（`config/agent.config` 或系统属性）

| 项 | 建议值 | 说明 |
|----|--------|------|
| `agent.sample_n_per_3_secs` | 本地 `-1`（全采样）；生产 `5000~10000` | 控制 Trace 采样 |
| `plugin.dubbo.collect_consumer_arguments` | `true`（仅内网） | Dubbo 消费端参数（注意脱敏） |
| `plugin.jdbc.trace_sql_parameters` | `false`（生产） | 避免 SQL 参数进 Span |
| `correlation.element_max_number` | `3` | 与日志 TraceId 关联时用 SW8 |

Dubbo 3 Triple 走 Agent 内置 Dubbo 插件；网关到 payment 若走 **内网 HTTP**（`PaymentGatewayClient`），会呈现为 HTTP Client Span，与 Dubbo Span 并列，属预期现象。

**Gateway 必开可选插件**：`mdyaipay-gateway` 是 Spring Cloud Gateway 4.x（WebFlux），对应插件不在默认 `plugins/`。`download-skywalking-agent.sh` 会把 `apm-spring-cloud-gateway-4.x-plugin` 与 `apm-spring-webflux-6.x-plugin` 拷入 `plugins/`。未拷贝时 OAP 仍能看到服务心跳（`mdyaipay-gateway::local`），但 **Trace/Endpoint/拓扑入口为空**。启用后须重启 gateway 进程。

### 5.4 与 `run-payment-loadtest.sh` 联调

1. `./scripts/run-skywalking.sh`
2. 分别用 Agent 启动 user、payment、gateway（或仅 payment 做链路验证）
3. 跑 `./scripts/run-payment-loadtest.sh` 或 gateway-collect 场景
4. 在 UI **Topology / Trace** 查看 `mdyaipay-gateway` → `mdyaipay-user` / `mdyaipay-payment`

## 6. 生产演进（概要）

1. **部署 OAP 集群**：BanyanDB 或对象存储 + OAP 水平扩展；UI 独立 Deployment。
2. **Agent 下发**：K8s 通过 `JAVA_TOOL_OPTIONS` 或 Init Container 挂载 Agent 目录；镜像内嵌 Agent 版本与 OAP 锁定同一 minor。
3. **采样与存储**：按入口 QPS 调 `sample_n_per_3_secs`；设置 Trace 保留天数与 BanyanDB 磁盘。
4. **安全**：11800/12800 仅内网；UI OAuth；关闭 SQL 参数、Dubbo 敏感参数上报。
5. **告警**：OAP 告警规则或导出至 Prometheus/Alertmanager（`SW_TELEMETRY=prometheus` 已开）。

## 7. 验收标准（本地）

- [ ] `run-skywalking.sh` 后 UI 可打开且无 OAP 连接错误
- [ ] 带 Agent 启动 payment，UI **General-Service** 出现 `mdyaipay-payment::local`
- [ ] 一次 collect 请求在 Trace 中可见 JDBC/Dubbo（或 HTTP）Span，耗时与 TimeTrace 量级一致
- [ ] 三服务均挂 Agent 时 Topology 显示 gateway → user/payment

## 8. 版本对齐

| 组件 | 当前 pin |
|------|----------|
| OAP / UI | 10.2.0 |
| BanyanDB | 0.8.0（与 OAP 10.2.x 匹配） |
| Java Agent | 9.7.0（`SKYWALKING_AGENT_VERSION`；与 OAP 独立发版） |

升级 OAP 时对照 [兼容矩阵](https://skywalking.apache.org/docs/) 调整 BanyanDB；Agent 升级见 [Java Agent 下载页](https://skywalking.apache.org/downloads/)。
