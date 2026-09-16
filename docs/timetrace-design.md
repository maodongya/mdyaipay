# @TimeTrace 设计（mdyaipay-tools-timetrace）

## 目标

在 Spring 管理的 Bean 上标注 `@TimeTrace`，在一次入口调用结束后输出：

1. **入口方法** wall-clock 总耗时；
2. 可选的慢调用门槛（`reportThresholdMillis`）与自定义报告回调。

基于 **Spring AOP**（`@Aspect` + `spring-boot-starter-aop`），不对业务模块做 AspectJ 编译期织入。

## 模块

- Artifact：`mdyaipay-tools-timetrace`
- 包：`com.mdyaipay.tools.timetrace`
- 依赖：`spring-aop`（AspectJ 注解风格，运行时由 Spring 代理织入）
- 不依赖业务模块；业务模块按需引入本 artifact。

## 组件

| 类型 | 职责 |
|------|------|
| `@TimeTrace` | 标记入口（`TYPE` / `METHOD`），可选 `value`、`reportOnComplete`、`reportThresholdMillis` |
| `TimeTraceAspect` | Spring `@Aspect`：拦截带注解的 Bean 方法 |
| `TimeTraceEntrySupport` | 入口 advice 逻辑（会话、计时、回调） |
| `TimeTraceContext` / `TimeTraceNode` | 线程内状态与报告根节点 |
| `TimeTraceReport` | 汇总结果 |
| `TimeTraceListener` / `TimeTraceReportFormatter` | 报告输出 |
| `TimeTraceSupport` | 全局 `TimeTraceListener` 配置 |

## 接入（Spring Boot）

依赖：

```xml
<dependency>
    <groupId>com.mdyaipay</groupId>
    <artifactId>mdyaipay-tools-timetrace</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

`TimeTraceAutoConfiguration` 会通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动注册 `TimeTraceAspect`（在 `AopAutoConfiguration` 之后）。

可选配置：

```yaml
mdyaipay:
  timetrace:
    enabled: true   # false 时不注册切面 Bean
```

参考：`mdyaipay-payment`（仅依赖上述两个 artifact，无手写 Configuration）。

## 限制

- 仅 **Spring 容器中的 Bean** 的 **public** 方法；同类内部 `this.xxx()` 自调用不会被切面拦截。
- 不统计 private 子方法为独立节点（与旧 AspectJ `cflowbelow` 行为不同）。
- 异步线程内需在新入口再次标注 `@TimeTrace` 才单独成报告。
- 生产环境对热点路径谨慎开启，或配合 `reportThresholdMillis`。

## 测试

模块内 `TimeTraceAspectTest` 使用 `@ImportAutoConfiguration` 加载 `AopAutoConfiguration` 与 `TimeTraceAutoConfiguration`。

```bash
mvn -pl mdyaipay-tools/mdyaipay-tools-timetrace test
```

## 使用示例

```java
@TimeTrace(value = "收单创建", reportThresholdMillis = 200)
public PaymentOrder createAndPay(...) { ... }
```

```java
TimeTraceSupport.setListener(report -> logger.info(TimeTraceReportFormatter.format(report)));
```
