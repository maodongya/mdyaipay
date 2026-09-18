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
 * {@code spanLevel} 表示相对根 span 的深度：<strong>根为 1</strong>，每个子节点为父节点 {@code spanLevel + 1}。
 * W3C {@code traceparent} 不携带深度，入站续链时将远程父 span 视为根，故本段 {@code spanLevel=2}。
 * <b>不负责</b>上报 OAP 或写 MDC。
 */
public final class TraceSnapshot {

    /** 根 span 的层级，固定为 1。 */
    static final int ROOT_SPAN_LEVEL = 1;

    /** 全链不变的 W3C traceId（32 位小写 hex）。 */
    private final String traceId;

    /** 当前段 span 标识（16 位小写 hex）。 */
    private final String spanId;

    /** 上游 spanId；根 span 为 {@code null}。 */
    private final String parentSpanId;

    /** 相对根 span 的深度：根为 1，子节点为父节点 + 1。 */
    private final int spanLevel;

    /** 是否与 W3C trace-flags 采样位一致。 */
    private final boolean sampled;

    /** SkyWalking {@code sw8} 原串；无则 {@code null}。 */
    private final String sw8;

    /** 非 PII 行李键值；不可变。 */
    private final Map<String, String> baggage;

    /**
     * 组装不可变快照。
     *
     * @param traceId      32 位 hex，全链不变
     * @param spanId       本段 span
     * @param parentSpanId 父 span；根为 {@code null}
     * @param spanLevel    根为 1，子节点须 ≥ 2
     * @param sampled      采样位
     * @param sw8          可选 sw8 原串
     * @param baggage      可选行李；{@code null} 或空视为无
     */
    private TraceSnapshot(
            String traceId,
            String spanId,
            String parentSpanId,
            int spanLevel,
            boolean sampled,
            String sw8,
            Map<String, String> baggage) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.spanLevel = requireSpanLevel(parentSpanId, spanLevel);
        this.sampled = sampled;
        this.sw8 = sw8;
        this.baggage = baggage == null || baggage.isEmpty() ? Map.of() : Map.copyOf(baggage);
    }

    /**
     * 新根 trace，默认 sampled=true，spanLevel=1。
     *
     * <p>幂等：每次调用生成新的 traceId/spanId，不读取当前线程上下文。
     */
    public static TraceSnapshot startNew() {
        return startNew(true);
    }

    /**
     * 新根 trace，spanLevel=1。
     *
     * @param sampled 是否与 W3C trace-flags 采样位一致
     */
    public static TraceSnapshot startNew(boolean sampled) {
        return new TraceSnapshot(
                IdGenerator.newTraceId(),
                IdGenerator.newSpanId(),
                null,
                ROOT_SPAN_LEVEL,
                sampled,
                null,
                Map.of());
    }

    /**
     * 显式字段组装（Outbox 还原、单测）。无 parent 时 spanLevel=1，有 parent 时按父为根记为 2。
     *
     * @param traceId      已有 traceId
     * @param spanId       本段 spanId
     * @param parentSpanId 父 span；根传 {@code null}
     * @param sampled      采样位
     */
    public static TraceSnapshot of(String traceId, String spanId, String parentSpanId, boolean sampled) {
        int spanLevel = parentSpanId == null ? ROOT_SPAN_LEVEL : ROOT_SPAN_LEVEL + 1;
        return of(traceId, spanId, parentSpanId, sampled, spanLevel);
    }

    /**
     * 显式字段组装，含 span 树深度（还原深层节点）。
     *
     * @param spanLevel 根必须为 1；有 parent 时必须 ≥ 2
     */
    public static TraceSnapshot of(
            String traceId, String spanId, String parentSpanId, boolean sampled, int spanLevel) {
        TraceIds.requireTraceId(traceId);
        TraceIds.requireSpanId(spanId);
        if (parentSpanId != null) {
            TraceIds.requireSpanId(parentSpanId);
        }
        return new TraceSnapshot(traceId, spanId, parentSpanId, spanLevel, sampled, null, Map.of());
    }

    /**
     * 从 inbound {@code traceparent} 继续：保留 traceId，生成本段新 spanId，父 span 为报头中的 spanId。
     * <p>
     * 报头无深度信息，远程父 span 视为根，故本段 spanLevel=2。
     */
    public static TraceSnapshot continueFromTraceParent(String traceParentHeader) {
        TraceparentCodec.Parsed parsed = TraceparentCodec.parse(traceParentHeader);
        return new TraceSnapshot(
                parsed.traceId(),
                IdGenerator.newSpanId(),
                parsed.spanId(),
                ROOT_SPAN_LEVEL + 1,
                parsed.sampled(),
                null,
                Map.of());
    }

    /**
     * 本段作为父节点，生成子 span（跨线程或出站前）。
     *
     * <p>副作用：新 spanId；traceId/sampled/sw8/baggage 不变；spanLevel = 本段 + 1。
     */
    public TraceSnapshot childSpan() {
        return new TraceSnapshot(traceId, IdGenerator.newSpanId(), spanId, spanLevel + 1, sampled, sw8, baggage);
    }

    /**
     * 附带 SkyWalking sw8 原串（与 Agent 并存时透传）。
     *
     * <p>幂等：空白值返回 this；非空则拷贝其余字段（含 spanLevel）。
     */
    public TraceSnapshot withSw8(String sw8Value) {
        if (sw8Value == null || sw8Value.isBlank()) {
            return this;
        }
        return new TraceSnapshot(traceId, spanId, parentSpanId, spanLevel, sampled, sw8Value.trim(), baggage);
    }

    /**
     * 合并 Baggage（键值须非 PII，数量见 {@code TraceStateCodec} 限制）。
     *
     * <p>幂等：空 map 返回 this；否则覆盖同名键。不改变 spanLevel。
     */
    public TraceSnapshot withBaggage(Map<String, String> entries) {
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) {
            return this;
        }
        Map<String, String> merged = new LinkedHashMap<>(baggage);
        merged.putAll(entries);
        return new TraceSnapshot(traceId, spanId, parentSpanId, spanLevel, sampled, sw8, Map.copyOf(merged));
    }

    /**
     * 全链 traceId。
     */
    public String traceId() {
        return traceId;
    }

    /**
     * 本段 spanId。
     */
    public String spanId() {
        return spanId;
    }

    /**
     * 上游 spanId；根 span 为 {@code null}。
     */
    public String parentSpanId() {
        return parentSpanId;
    }

    /**
     * 相对根 span 的深度：根为 1，子节点为父节点 + 1。
     */
    public int spanLevel() {
        return spanLevel;
    }

    /**
     * 是否采样。
     */
    public boolean sampled() {
        return sampled;
    }

    /**
     * sw8 原串；无则 {@code null}。
     */
    public String sw8() {
        return sw8;
    }

    /**
     * 行李键值（不可变，可能为空 map）。
     */
    public Map<String, String> baggage() {
        return baggage;
    }

    /**
     * 格式化为 W3C {@code traceparent} 供出站注入。
     *
     * <p>幂等：同一快照多次调用结果相同。不写入 spanLevel（协议无此字段）。
     */
    public String toTraceParentHeader() {
        return TraceparentCodec.format(traceId, spanId, sampled);
    }

    /**
     * 校验 spanLevel 与 parentSpanId 一致：根必须为 1 且无 parent；子节点必须 ≥ 2 且有 parent。
     *
     * @return 合法的 spanLevel
     */
    private static int requireSpanLevel(String parentSpanId, int spanLevel) {
        if (spanLevel < ROOT_SPAN_LEVEL) {
            throw new IllegalArgumentException("spanLevel 须 >= 1");
        }
        if (parentSpanId == null) {
            if (spanLevel != ROOT_SPAN_LEVEL) {
                throw new IllegalArgumentException("根 span 的 spanLevel 必须为 1");
            }
            return spanLevel;
        }
        if (spanLevel < ROOT_SPAN_LEVEL + 1) {
            throw new IllegalArgumentException("子 span 的 spanLevel 须 >= 2");
        }
        return spanLevel;
    }
}
