package com.mdyaipay.tools.trace;

import com.mdyaipay.tools.trace.internal.IdGenerator;
import com.mdyaipay.tools.trace.internal.TraceparentCodec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 某一时刻的 Trace 快照，可绑定到 {@link TraceContext} 或写入 {@link TextMapCarrier}。
 * <p>
 * 不可变。跨进程/线程边界时用 {@link #childSpan()} 生成新 spanId，{@code traceId} 不变。
 * {@code spanLevel} 表示<strong>本服务内</strong>深度（入口恒为 {@link #ROOT_SPAN_LEVEL}，子节点 +1）。
 * {@code serverDepthLevel} 表示<strong>全链服务跳数</strong>（Gateway=1、Payment=2、User=3），同服务内切片不变。
 * {@code spanLevelGlobal} 表示<strong>全链 span 深度</strong>，跨方法与跨服务均 +1。
 * <b>不负责</b>上报 OAP 或写 MDC。
 */
public final class TraceSnapshot {

    /** 本服务内根 span 的层级，固定为 1。 */
    public static final int ROOT_SPAN_LEVEL = 1;

    /** 全链首跳服务深度（无上游时），固定为 1。 */
    public static final int ROOT_SERVER_DEPTH_LEVEL = 1;

    /** 全链首 span 深度（无上游时），固定为 1。 */
    public static final int ROOT_SPAN_LEVEL_GLOBAL = 1;

    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final int spanLevel;
    private final int serverDepthLevel;
    private final int spanLevelGlobal;
    private final boolean sampled;
    private final String sw8;
    private final Map<String, String> baggage;

    private TraceSnapshot(
            String traceId,
            String spanId,
            String parentSpanId,
            int spanLevel,
            int serverDepthLevel,
            int spanLevelGlobal,
            boolean sampled,
            String sw8,
            Map<String, String> baggage) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.spanLevel = requireSpanLevel(parentSpanId, spanLevel);
        this.serverDepthLevel = requireServerDepthLevel(serverDepthLevel);
        this.spanLevelGlobal = requireSpanLevelGlobal(spanLevelGlobal);
        this.sampled = sampled;
        this.sw8 = sw8;
        this.baggage = baggage == null || baggage.isEmpty() ? Map.of() : Map.copyOf(baggage);
    }

    /**
     * 新根 trace，默认 sampled=true；各深度字段均为 1。
     *
     * <p>幂等：每次调用生成新的 traceId/spanId，不读取当前线程上下文。
     */
    public static TraceSnapshot startNew() {
        return startNew(true);
    }

    /**
     * 新根 trace。
     *
     * @param sampled 是否与 W3C trace-flags 采样位一致
     */
    public static TraceSnapshot startNew(boolean sampled) {
        return new TraceSnapshot(
                IdGenerator.newTraceId(),
                IdGenerator.newSpanId(),
                null,
                ROOT_SPAN_LEVEL,
                ROOT_SERVER_DEPTH_LEVEL,
                ROOT_SPAN_LEVEL_GLOBAL,
                sampled,
                null,
                Map.of());
    }

    /**
     * 显式字段组装（Outbox 还原、单测）：本服务内入口 spanLevel=1；有 parent 时使用续链默认深度。
     */
    public static TraceSnapshot of(String traceId, String spanId, String parentSpanId, boolean sampled) {
        int serverDepth = parentSpanId == null ? ROOT_SERVER_DEPTH_LEVEL : defaultContinuedServerDepthLevel();
        int global = parentSpanId == null ? ROOT_SPAN_LEVEL_GLOBAL : defaultContinuedSpanLevelGlobal();
        return of(traceId, spanId, parentSpanId, sampled, ROOT_SPAN_LEVEL, serverDepth, global);
    }

    /**
     * 显式字段组装，含本服务内 span 深度与全链深度。
     *
     * @param spanLevel        无 parent 时必须为 1
     * @param serverDepthLevel 全链服务跳数，须 ≥ 1
     * @param spanLevelGlobal  全链 span 深度，须 ≥ 1
     */
    public static TraceSnapshot of(
            String traceId,
            String spanId,
            String parentSpanId,
            boolean sampled,
            int spanLevel,
            int serverDepthLevel,
            int spanLevelGlobal) {
        TraceIds.requireTraceId(traceId);
        TraceIds.requireSpanId(spanId);
        if (parentSpanId != null) {
            TraceIds.requireSpanId(parentSpanId);
        }
        return new TraceSnapshot(
                traceId,
                spanId,
                parentSpanId,
                spanLevel,
                serverDepthLevel,
                spanLevelGlobal,
                sampled,
                null,
                Map.of());
    }

    /**
     * 从 inbound {@code traceparent} 继续；全链深度由 {@link Propagation#extract} 从 tracestate 写入。
     */
    public static TraceSnapshot continueFromTraceParent(String traceParentHeader) {
        TraceparentCodec.Parsed parsed = TraceparentCodec.parse(traceParentHeader);
        return new TraceSnapshot(
                parsed.traceId(),
                IdGenerator.newSpanId(),
                parsed.spanId(),
                ROOT_SPAN_LEVEL,
                defaultContinuedServerDepthLevel(),
                defaultContinuedSpanLevelGlobal(),
                parsed.sampled(),
                null,
                Map.of());
    }

    /**
     * 续链且无 tracestate {@link TraceBaggageKeys#SERVER_DEPTH_LEVEL} 时的默认全链服务深度。
     */
    public static int defaultContinuedServerDepthLevel() {
        return ROOT_SERVER_DEPTH_LEVEL + 1;
    }

    /**
     * 续链且无 tracestate {@link TraceBaggageKeys#SPAN_LEVEL_GLOBAL} 时的默认全链 span 深度。
     */
    public static int defaultContinuedSpanLevelGlobal() {
        return ROOT_SPAN_LEVEL_GLOBAL + 1;
    }

    /**
     * 续链时本服务 HTTP/Dubbo 入口的 span 层级（恒为根）。
     */
    public static int defaultContinuedEntrySpanLevel() {
        return ROOT_SPAN_LEVEL;
    }

    /**
     * 本段作为父节点，生成子 span（跨线程或进程内切片）。
     *
     * <p>副作用：新 spanId；{@code spanLevel + 1}、{@code spanLevelGlobal + 1}；{@code serverDepthLevel} 不变。
     */
    public TraceSnapshot childSpan() {
        return new TraceSnapshot(
                traceId,
                IdGenerator.newSpanId(),
                spanId,
                spanLevel + 1,
                serverDepthLevel,
                spanLevelGlobal + 1,
                sampled,
                sw8,
                baggage);
    }

    /**
     * 附带 SkyWalking sw8 原串（与 Agent 并存时透传）。
     */
    public TraceSnapshot withSw8(String sw8Value) {
        if (sw8Value == null || sw8Value.isBlank()) {
            return this;
        }
        return new TraceSnapshot(
                traceId,
                spanId,
                parentSpanId,
                spanLevel,
                serverDepthLevel,
                spanLevelGlobal,
                sampled,
                sw8Value.trim(),
                baggage);
    }

    /**
     * 合并 Baggage（键值须非 PII）；不改变 spanLevel / serverDepthLevel / spanLevelGlobal。
     */
    public TraceSnapshot withBaggage(Map<String, String> entries) {
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) {
            return this;
        }
        Map<String, String> merged = new LinkedHashMap<>(baggage);
        merged.putAll(entries);
        return new TraceSnapshot(
                traceId,
                spanId,
                parentSpanId,
                spanLevel,
                serverDepthLevel,
                spanLevelGlobal,
                sampled,
                sw8,
                Map.copyOf(merged));
    }

    public String traceId() {
        return traceId;
    }

    public String spanId() {
        return spanId;
    }

    public String parentSpanId() {
        return parentSpanId;
    }

    /** 本服务内 span 深度：入口为 1，子切片逐层递增。 */
    public int spanLevel() {
        return spanLevel;
    }

    /** 全链服务跳数深度：Gateway=1，每跨服务 +1，同服务内不变。 */
    public int serverDepthLevel() {
        return serverDepthLevel;
    }

    /** 全链 span 深度：跨方法与跨服务均累计 +1。 */
    public int spanLevelGlobal() {
        return spanLevelGlobal;
    }

    public boolean sampled() {
        return sampled;
    }

    public String sw8() {
        return sw8;
    }

    public Map<String, String> baggage() {
        return baggage;
    }

    /**
     * 格式化为 W3C {@code traceparent} 供出站注入。
     */
    public String toTraceParentHeader() {
        return TraceparentCodec.format(traceId, spanId, sampled);
    }

    private static int requireSpanLevel(String parentSpanId, int spanLevel) {
        if (spanLevel < ROOT_SPAN_LEVEL) {
            throw new IllegalArgumentException("spanLevel 须 >= 1");
        }
        if (parentSpanId == null && spanLevel != ROOT_SPAN_LEVEL) {
            throw new IllegalArgumentException("无 parent 时 spanLevel 必须为 1");
        }
        return spanLevel;
    }

    private static int requireServerDepthLevel(int serverDepthLevel) {
        if (serverDepthLevel < ROOT_SERVER_DEPTH_LEVEL) {
            throw new IllegalArgumentException("serverDepthLevel 须 >= 1");
        }
        return serverDepthLevel;
    }

    private static int requireSpanLevelGlobal(int spanLevelGlobal) {
        if (spanLevelGlobal < ROOT_SPAN_LEVEL_GLOBAL) {
            throw new IllegalArgumentException("spanLevelGlobal 须 >= 1");
        }
        return spanLevelGlobal;
    }
}
