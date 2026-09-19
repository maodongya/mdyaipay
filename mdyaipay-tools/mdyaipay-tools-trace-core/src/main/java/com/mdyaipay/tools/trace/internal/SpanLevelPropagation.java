package com.mdyaipay.tools.trace.internal;

import com.mdyaipay.tools.trace.TraceBaggageKeys;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * 经 {@code tracestate} 传递 {@code spanLevel=1}：标记下一跳以本服务根（level 1）接入；入站剥离该键。
 */
public final class SpanLevelPropagation {

    /** 与 {@link com.mdyaipay.tools.trace.TraceSnapshot} 根层级一致。 */
    private static final int MIN_SPAN_LEVEL = 1;

    private SpanLevelPropagation() {
    }

    /**
     * 计算出站应写入的「下游服务入口」层级。
     *
     * @param currentSpanLevel 当前上下文快照层级
     * @return 下游入口层级（当前 + 1）
     */
    public static int downstreamEntryLevel(int currentSpanLevel) {
        if (currentSpanLevel < MIN_SPAN_LEVEL) {
            throw new IllegalArgumentException("currentSpanLevel 须 >= 1");
        }
        return currentSpanLevel + 1;
    }

    /**
     * 从已解码的 tracestate 行李中读取入口 spanLevel。
     *
     * @param baggage {@link com.mdyaipay.tools.trace.internal.TraceStateCodec#decode} 结果
     * @return 合法正整数；缺失或非法时 empty
     */
    public static OptionalInt readEntryLevel(Map<String, String> baggage) {
        if (baggage == null || baggage.isEmpty()) {
            return OptionalInt.empty();
        }
        String raw = baggage.get(TraceBaggageKeys.SPAN_LEVEL);
        if (raw == null || raw.isBlank()) {
            return OptionalInt.empty();
        }
        try {
            int level = Integer.parseInt(raw.trim());
            if (level < MIN_SPAN_LEVEL) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(level);
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    /**
     * 拷贝行李并去掉 {@link TraceBaggageKeys#SPAN_LEVEL}，避免与快照字段重复存储。
     *
     * @param baggage 原始行李
     * @return 不含 spanLevel 键的新 map；无其它键时返回空 map
     */
    public static Map<String, String> withoutSpanLevel(Map<String, String> baggage) {
        if (baggage == null || baggage.isEmpty()) {
            return Map.of();
        }
        if (!baggage.containsKey(TraceBaggageKeys.SPAN_LEVEL)) {
            return Map.copyOf(baggage);
        }
        Map<String, String> copy = new LinkedHashMap<>(baggage);
        copy.remove(TraceBaggageKeys.SPAN_LEVEL);
        return copy.isEmpty() ? Map.of() : Map.copyOf(copy);
    }

    /**
     * 合并业务行李与出站 spanLevel，供 {@link TraceStateCodec#encode} 使用。
     *
     * @param baggage         快照上的业务行李
     * @param downstreamLevel 下游入口层级
     * @return 可编码 map
     */
    public static Map<String, String> forTraceStateEncode(Map<String, String> baggage, int downstreamLevel) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (baggage != null && !baggage.isEmpty()) {
            merged.putAll(baggage);
        }
        merged.put(TraceBaggageKeys.SPAN_LEVEL, Integer.toString(downstreamLevel));
        return Map.copyOf(merged);
    }
}
