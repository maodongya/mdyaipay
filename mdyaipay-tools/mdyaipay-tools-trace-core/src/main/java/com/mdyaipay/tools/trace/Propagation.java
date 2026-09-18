package com.mdyaipay.tools.trace;

import com.mdyaipay.tools.trace.internal.Sw8Fallback;
import com.mdyaipay.tools.trace.internal.TraceStateCodec;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 跨边界 Trace 注入与提取：W3C {@code traceparent} 优先，{@code sw8} 兜底。
 * <p>
 * <b>幂等：</b>重复 inject 覆盖同键；extract 不修改 Carrier。业务代码默认由 Filter/拦截器调用，勿手写 inject。
 */
public final class Propagation {

    private Propagation() {
    }

    /**
     * 将当前 {@link TraceContext} 写入 Carrier。
     *
     * @return 是否写入（无上下文时为 false）
     */
    public static boolean inject(TextMapCarrier carrier) {
        Objects.requireNonNull(carrier, "carrier");
        return TraceContext.current().map(snapshot -> {
            inject(carrier, snapshot);
            return true;
        }).orElse(false);
    }

    /**
     * 将快照写入 Carrier（出站 HTTP/Dubbo/MQ）。
     */
    public static void inject(TextMapCarrier carrier, TraceSnapshot snapshot) {
        Objects.requireNonNull(carrier, "carrier");
        Objects.requireNonNull(snapshot, "snapshot");
        carrier.set(TraceHeaders.TRACE_PARENT, snapshot.toTraceParentHeader());
        String traceState = TraceStateCodec.encode(snapshot.baggage());
        if (traceState != null) {
            carrier.set(TraceHeaders.TRACE_STATE, traceState);
        }
        if (snapshot.sw8() != null) {
            carrier.set(TraceHeaders.SW8, snapshot.sw8());
        }
    }

    /**
     * 从 Carrier 提取并生成本段快照（新 spanId，traceId 延续）。
     */
    public static Optional<TraceSnapshot> extract(TextMapCarrier carrier) {
        Objects.requireNonNull(carrier, "carrier");
        Optional<TraceSnapshot> fromW3c = extractTraceParent(carrier);
        if (fromW3c.isPresent()) {
            return fromW3c;
        }
        return Sw8Fallback.continueTrace(carrier.get(TraceHeaders.SW8))
                .map(snapshot -> mergeTraceState(carrier, snapshot));
    }

    private static Optional<TraceSnapshot> extractTraceParent(TextMapCarrier carrier) {
        String header = carrier.get(TraceHeaders.TRACE_PARENT);
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        try {
            TraceSnapshot snapshot = TraceSnapshot.continueFromTraceParent(header);
            return Optional.of(mergeTraceState(carrier, mergeSw8(carrier, snapshot)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static TraceSnapshot mergeSw8(TextMapCarrier carrier, TraceSnapshot snapshot) {
        if (snapshot.sw8() != null) {
            return snapshot;
        }
        String sw8 = carrier.get(TraceHeaders.SW8);
        return sw8 == null || sw8.isBlank() ? snapshot : snapshot.withSw8(sw8);
    }

    private static TraceSnapshot mergeTraceState(TextMapCarrier carrier, TraceSnapshot snapshot) {
        Map<String, String> baggage = TraceStateCodec.decode(carrier.get(TraceHeaders.TRACE_STATE));
        return baggage.isEmpty() ? snapshot : snapshot.withBaggage(baggage);
    }
}
