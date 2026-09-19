package com.mdyaipay.tools.trace;

/**
 * HTTP 头、Dubbo attachment、RocketMQ UserProperty 共用的 Trace 键名。
 * <p>
 * 与 W3C Trace Context 及 SkyWalking Java Agent 对齐；禁止业务自定义别名。
 */
public final class TraceHeaders {

    /** W3C {@code traceparent}。 */
    public static final String TRACE_PARENT = "traceparent";

    /** W3C {@code tracestate}（轻量 Baggage）。 */
    public static final String TRACE_STATE = "tracestate";

    /** SkyWalking {@code sw8}。 */
    public static final String SW8 = "sw8";

    private TraceHeaders() {
    }
}
