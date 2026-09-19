package com.mdyaipay.tools.trace;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * W3C {@code traceId} / {@code spanId} 的格式校验与展示转换。
 * <p>
 * 不负责线程绑定；绑定见 {@link TraceContext}。
 */
public final class TraceIds {

    private static final Pattern TRACE_ID = Pattern.compile("^[0-9a-f]{32}$");
    private static final Pattern SPAN_ID = Pattern.compile("^[0-9a-f]{16}$");

    private TraceIds() {
    }

    /**
     * 校验 128-bit traceId（32 位小写 hex，且非全 0）。
     */
    public static void requireTraceId(String traceId) {
        Objects.requireNonNull(traceId, "traceId");
        if (!TRACE_ID.matcher(traceId).matches()) {
            throw new IllegalArgumentException("traceId 须为 32 位小写 hex");
        }
        if (isAllZero(traceId)) {
            throw new IllegalArgumentException("traceId 不能为全 0");
        }
    }

    /**
     * 校验 64-bit spanId（16 位小写 hex，且非全 0）。
     */
    public static void requireSpanId(String spanId) {
        Objects.requireNonNull(spanId, "spanId");
        if (!SPAN_ID.matcher(spanId).matches()) {
            throw new IllegalArgumentException("spanId 须为 16 位小写 hex");
        }
        if (isAllZero(spanId)) {
            throw new IllegalArgumentException("spanId 不能为全 0");
        }
    }

    /**
     * API 响应用短 traceId：完整 32 位 hex 的<strong>后 12 位</strong>（与历史 {@code ApiResponse} 长度一致）。
     */
    public static String toDisplayTraceId(String fullTraceId) {
        requireTraceId(fullTraceId);
        return fullTraceId.substring(fullTraceId.length() - 12);
    }

    static boolean isAllZero(String hex) {
        for (int i = 0; i < hex.length(); i++) {
            if (hex.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }
}
