package com.mdyaipay.tools.trace;

import com.mdyaipay.tools.trace.internal.TraceparentCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceparentCodecTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";

    @Test
    void formatsAndParsesSampledHeader() {
        String header = TraceparentCodec.format(TRACE_ID, SPAN_ID, true);
        assertEquals("00-" + TRACE_ID + "-" + SPAN_ID + "-01", header);

        TraceparentCodec.Parsed parsed = TraceparentCodec.parse(header);
        assertEquals(TRACE_ID, parsed.traceId());
        assertEquals(SPAN_ID, parsed.spanId());
        assertTrue(parsed.sampled());
    }

    @Test
    void rejectsInvalidHeader() {
        assertThrows(IllegalArgumentException.class, () -> TraceparentCodec.parse("invalid"));
        assertThrows(IllegalArgumentException.class, () -> TraceparentCodec.parse(
                "00-" + "0".repeat(32) + "-" + SPAN_ID + "-01"));
    }

    @Test
    void continueFromTraceParentCreatesNewSpan() {
        String incoming = TraceparentCodec.format(TRACE_ID, SPAN_ID, false);
        TraceSnapshot snapshot = TraceSnapshot.continueFromTraceParent(incoming);
        assertEquals(TRACE_ID, snapshot.traceId());
        assertEquals(SPAN_ID, snapshot.parentSpanId());
        assertFalse(snapshot.sampled());
        assertTrue(!snapshot.spanId().equals(SPAN_ID));
    }
}
