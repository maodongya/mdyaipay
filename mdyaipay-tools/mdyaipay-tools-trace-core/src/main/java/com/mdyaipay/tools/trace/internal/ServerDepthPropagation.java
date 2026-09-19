package com.mdyaipay.tools.trace.internal;

import com.mdyaipay.tools.trace.TraceBaggageKeys;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * 经 {@code tracestate} 传递全链深度：{@link TraceBaggageKeys#SERVER_DEPTH_LEVEL}、{@link TraceBaggageKeys#SPAN_LEVEL_GLOBAL}。
 */
public final class ServerDepthPropagation {

    private static final int MIN_DEPTH = 1;

    private ServerDepthPropagation() {
    }

    /**
     * 计算出站应写入的下游服务 {@code serverDepthLevel}（当前 + 1）。
     *
     * @param currentServerDepth 当前快照上的全链服务深度
     * @return 下游入口深度
     */
    public static int downstreamServerDepth(int currentServerDepth) {
        return downstreamLevel(currentServerDepth);
    }

    /**
     * 计算出站应写入的下游 {@code spanLevelGlobal}（当前 + 1，与跨方法 childSpan 语义一致）。
     *
     * @param currentSpanLevelGlobal 当前快照上的全链 span 深度
     * @return 下游入口全链 span 深度
     */
    public static int downstreamSpanLevelGlobal(int currentSpanLevelGlobal) {
        return downstreamLevel(currentSpanLevelGlobal);
    }

    /**
     * 从 tracestate 解码结果读取本服务入口 {@code serverDepthLevel}。
     *
     * @param baggage 行李 map
     * @return 合法正整数；缺失或非法时 empty
     */
    public static OptionalInt readEntryServerDepth(Map<String, String> baggage) {
        return readPositiveInt(baggage, TraceBaggageKeys.SERVER_DEPTH_LEVEL);
    }

    /**
     * 从 tracestate 解码结果读取本服务入口 {@code spanLevelGlobal}。
     *
     * @param baggage 行李 map
     * @return 合法正整数；缺失或非法时 empty
     */
    public static OptionalInt readEntrySpanLevelGlobal(Map<String, String> baggage) {
        return readPositiveInt(baggage, TraceBaggageKeys.SPAN_LEVEL_GLOBAL);
    }

    /**
     * 剥离框架保留键，保留业务行李。
     *
     * @param baggage 原始行李
     * @return 不含框架键的 map
     */
    public static Map<String, String> withoutFrameworkKeys(Map<String, String> baggage) {
        if (baggage == null || baggage.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>(baggage);
        copy.remove(TraceBaggageKeys.SERVER_DEPTH_LEVEL);
        copy.remove(TraceBaggageKeys.SPAN_LEVEL_GLOBAL);
        copy.remove(TraceBaggageKeys.SPAN_LEVEL);
        return copy.isEmpty() ? Map.of() : Map.copyOf(copy);
    }

    /**
     * 合并业务行李与出站全链深度，供 {@link TraceStateCodec#encode} 使用。
     *
     * @param baggage                   快照业务行李
     * @param downstreamServerDepth     下游入口全链服务深度
     * @param downstreamSpanLevelGlobal 下游入口全链 span 深度
     * @return 可编码 map
     */
    public static Map<String, String> forTraceStateEncode(
            Map<String, String> baggage, int downstreamServerDepth, int downstreamSpanLevelGlobal) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (baggage != null && !baggage.isEmpty()) {
            merged.putAll(withoutFrameworkKeys(baggage));
        }
        merged.put(TraceBaggageKeys.SERVER_DEPTH_LEVEL, Integer.toString(downstreamServerDepth));
        merged.put(TraceBaggageKeys.SPAN_LEVEL_GLOBAL, Integer.toString(downstreamSpanLevelGlobal));
        return Map.copyOf(merged);
    }

    private static int downstreamLevel(int current) {
        if (current < MIN_DEPTH) {
            throw new IllegalArgumentException("current 须 >= 1");
        }
        return current + 1;
    }

    private static OptionalInt readPositiveInt(Map<String, String> baggage, String key) {
        if (baggage == null || baggage.isEmpty()) {
            return OptionalInt.empty();
        }
        String raw = baggage.get(key);
        if (raw == null || raw.isBlank()) {
            return OptionalInt.empty();
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < MIN_DEPTH) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(value);
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }
}
