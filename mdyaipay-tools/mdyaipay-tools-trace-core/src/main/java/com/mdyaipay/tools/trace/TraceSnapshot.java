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
 * <b>不负责</b>上报 OAP 或写 MDC。
 */
public final class TraceSnapshot {

    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final boolean sampled;
    private final String sw8;
    private final Map<String, String> baggage;

    private TraceSnapshot(
            String traceId,
            String spanId,
            String parentSpanId,
            boolean sampled,
            String sw8,
            Map<String, String> baggage) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.sampled = sampled;
        this.sw8 = sw8;
        this.baggage = baggage == null || baggage.isEmpty() ? Map.of() : Map.copyOf(baggage);
    }

    /** 新根 trace，默认 sampled=true。 */
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
                sampled,
                null,
                Map.of());
    }

    /**
     * 显式字段组装（Outbox 还原、单测）。
     */
    public static TraceSnapshot of(String traceId, String spanId, String parentSpanId, boolean sampled) {
        TraceIds.requireTraceId(traceId);
        TraceIds.requireSpanId(spanId);
        if (parentSpanId != null) {
            TraceIds.requireSpanId(parentSpanId);
        }
        return new TraceSnapshot(traceId, spanId, parentSpanId, sampled, null, Map.of());
    }

    /**
     * 从 inbound {@code traceparent} 继续：保留 traceId，生成本段新 spanId，父 span 为报头中的 spanId。
     */
    public static TraceSnapshot continueFromTraceParent(String traceParentHeader) {
        TraceparentCodec.Parsed parsed = TraceparentCodec.parse(traceParentHeader);
        return new TraceSnapshot(
                parsed.traceId(),
                IdGenerator.newSpanId(),
                parsed.spanId(),
                parsed.sampled(),
                null,
                Map.of());
    }

    /** 本段作为父节点，生成子 span（跨线程或出站前）。 */
    public TraceSnapshot childSpan() {
        return new TraceSnapshot(traceId, IdGenerator.newSpanId(), spanId, sampled, sw8, baggage);
    }

    /** 附带 SkyWalking sw8 原串（与 Agent 并存时透传）。 */
    public TraceSnapshot withSw8(String sw8Value) {
        if (sw8Value == null || sw8Value.isBlank()) {
            return this;
        }
        return new TraceSnapshot(traceId, spanId, parentSpanId, sampled, sw8Value.trim(), baggage);
    }

    /** 合并 Baggage（键值须非 PII，数量见 {@code TraceStateCodec} 限制）。 */
    public TraceSnapshot withBaggage(Map<String, String> entries) {
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) {
            return this;
        }
        Map<String, String> merged = new LinkedHashMap<>(baggage);
        merged.putAll(entries);
        return new TraceSnapshot(traceId, spanId, parentSpanId, sampled, sw8, Map.copyOf(merged));
    }

    public String traceId() {
        return traceId;
    }

    public String spanId() {
        return spanId;
    }

    /** 上游 spanId；根 span 为 null。 */
    public String parentSpanId() {
        return parentSpanId;
    }

    public boolean sampled() {
        return sampled;
    }

    /** sw8 原串；无则 null。 */
    public String sw8() {
        return sw8;
    }

    public Map<String, String> baggage() {
        return baggage;
    }

    /** 格式化为 W3C {@code traceparent} 供出站注入。 */
    public String toTraceParentHeader() {
        return TraceparentCodec.format(traceId, spanId, sampled);
    }
}
