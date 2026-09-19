package com.mdyaipay.tools.trace;

/**
 * {@link TraceHeaders#TRACE_STATE} 中与本框架 Trace 语义相关的固定键名。
 * <p>
 * 与业务自定义 Baggage 键区分；出站 {@link Propagation#inject} 会写入 {@link #SPAN_LEVEL}。
 */
public final class TraceBaggageKeys {

    /**
     * 出站 {@code tracestate} 标记：下游本服务入口为根（值固定为 {@code 1}）；入站时剥离，不参与累加全链深度。
     */
    public static final String SPAN_LEVEL = "spanLevel";

    private TraceBaggageKeys() {
    }
}
