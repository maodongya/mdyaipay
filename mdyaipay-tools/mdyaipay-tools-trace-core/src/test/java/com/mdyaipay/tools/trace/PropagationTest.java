package com.mdyaipay.tools.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PropagationTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void injectExtractRoundTrip() {
        TraceSnapshot root = TraceSnapshot.startNew();
        TraceContext.bind(root);

        TextMapCarrier outbound = TextMapCarrier.create();
        Propagation.inject(outbound);

        TraceContext.clear();
        TraceSnapshot extracted = Propagation.extract(outbound).orElseThrow();

        assertEquals(root.traceId(), extracted.traceId());
        assertNotEquals(root.spanId(), extracted.spanId());
        assertEquals(root.spanId(), extracted.parentSpanId());
        assertEquals(1, extracted.spanLevel());
        assertEquals(2, extracted.serverDepthLevel());
        assertEquals(2, extracted.spanLevelGlobal());
    }

    @Test
    void injectExtractUsesLocalRootOnDownstreamEntry() {
        TraceSnapshot caller = TraceSnapshot.startNew();
        TextMapCarrier outbound = TextMapCarrier.create();
        Propagation.inject(outbound, caller);

        TraceSnapshot callee = Propagation.extract(outbound).orElseThrow();
        assertEquals(1, callee.spanLevel());
        assertEquals(2, callee.serverDepthLevel());
        assertEquals(2, callee.spanLevelGlobal());
        assertEquals(caller.spanId(), callee.parentSpanId());
    }

    @Test
    void extractUsesServerDepthFromTraceState() {
        TraceSnapshot root = TraceSnapshot.startNew();
        TextMapCarrier carrier = TextMapCarrier.create();
        carrier.set(TraceHeaders.TRACE_PARENT, root.toTraceParentHeader());
        carrier.set(TraceHeaders.TRACE_STATE, TraceBaggageKeys.SERVER_DEPTH_LEVEL + "=3");

        TraceSnapshot extracted = Propagation.extract(carrier).orElseThrow();
        assertEquals(1, extracted.spanLevel());
        assertEquals(3, extracted.serverDepthLevel());
        assertEquals(root.spanId(), extracted.parentSpanId());
    }

    @Test
    void extractUsesSpanLevelGlobalFromTraceState() {
        TraceSnapshot root = TraceSnapshot.startNew();
        TextMapCarrier carrier = TextMapCarrier.create();
        carrier.set(TraceHeaders.TRACE_PARENT, root.toTraceParentHeader());
        carrier.set(TraceHeaders.TRACE_STATE, TraceBaggageKeys.SPAN_LEVEL_GLOBAL + "=5");

        TraceSnapshot extracted = Propagation.extract(carrier).orElseThrow();
        assertEquals(1, extracted.spanLevel());
        assertEquals(5, extracted.spanLevelGlobal());
    }

    @Test
    void injectIncrementsGlobalWhenCallerHadChildSpan() {
        TraceSnapshot root = TraceSnapshot.startNew().childSpan().childSpan();
        assertEquals(3, root.spanLevelGlobal());

        TextMapCarrier outbound = TextMapCarrier.create();
        Propagation.inject(outbound, root);

        TraceSnapshot callee = Propagation.extract(outbound).orElseThrow();
        assertEquals(1, callee.spanLevel());
        assertEquals(4, callee.spanLevelGlobal());
    }

    @Test
    void injectBaggageViaTraceState() {
        TraceSnapshot root = TraceSnapshot.startNew().withBaggage(Map.of("merchantId", "m-1"));
        TextMapCarrier carrier = TextMapCarrier.create();
        Propagation.inject(carrier, root);

        TraceSnapshot extracted = Propagation.extract(carrier).orElseThrow();
        assertEquals("m-1", extracted.baggage().get("merchantId"));
    }

    @Test
    void displayTraceIdUsesLastTwelveChars() {
        String full = "4bf92f3577b34da6a3ce929d0e0e4736";
        assertEquals("929d0e0e4736", TraceIds.toDisplayTraceId(full));
    }

    @Test
    void sw8PassthroughWhenNoTraceParent() {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String sw8 = "1-" + traceId + "-segment-span";
        TextMapCarrier carrier = TextMapCarrier.create();
        carrier.set(TraceHeaders.SW8, sw8);

        TraceSnapshot extracted = Propagation.extract(carrier).orElseThrow();
        assertEquals(traceId, extracted.traceId());
        assertEquals(sw8, extracted.sw8());
    }
}
