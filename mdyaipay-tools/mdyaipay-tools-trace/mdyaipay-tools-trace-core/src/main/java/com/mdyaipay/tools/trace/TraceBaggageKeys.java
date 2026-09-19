package com.mdyaipay.tools.trace;

/**
 * {@link TraceHeaders#TRACE_STATE} 中与本框架 Trace 语义相关的固定键名。
 * <p>
 * 与业务自定义 Baggage 键区分；出站 {@link Propagation#inject} 会写入框架深度键。
 */
public final class TraceBaggageKeys {

    /**
     * 历史 tracestate 键；入站时剥离，全链深度请用 {@link #SERVER_DEPTH_LEVEL} / {@link #SPAN_LEVEL_GLOBAL}。
     */
    public static final String SPAN_LEVEL = "spanLevel";

    /**
     * 全链服务跳数深度：首服务（如 Gateway）为 {@code 1}，每跨一跳 +1（Payment=2、User=3）。
     */
    public static final String SERVER_DEPTH_LEVEL = "serverDepthLevel";

    /**
     * 全链 span 深度：跨方法（{@link TraceSnapshot#childSpan()}）与跨服务（出站 inject）均 +1。
     */
    public static final String SPAN_LEVEL_GLOBAL = "spanLevelGlobal";

    private TraceBaggageKeys() {
    }
}
