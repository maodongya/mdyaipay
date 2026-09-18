/**
 * 分布式 Trace 上下文与跨边界传播（W3C {@code traceparent}、SkyWalking {@code sw8} 透传）。
 * <p>
 * 本模块<strong>仅 JDK</strong>，不负责 Span 上报、MDC 或 Spring/Web 集成（见 {@code mdyaipay-tools-trace-spring-boot}）。
 * 实现细节在 {@link com.mdyaipay.tools.trace.internal}，业务只依赖本包 public 类型。
 */
package com.mdyaipay.tools.trace;
