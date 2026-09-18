package com.mdyaipay.tools.trace;

import com.mdyaipay.tools.trace.internal.TraceparentCodec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 覆盖 {@link TraceSnapshot} 的 span 树层级：根为 1，子节点为父节点 + 1。
 */
class TraceSnapshotTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";
    private static final String PARENT_SPAN_ID = "00f067aa0ba902b8";

    /**
     * 新根 trace 的 spanLevel 必须为 1，且无父 span。
     */
    @Test
    void startNewUsesRootSpanLevel() {
        TraceSnapshot root = TraceSnapshot.startNew();
        assertEquals(1, root.spanLevel());
        assertNull(root.parentSpanId());
    }

    /**
     * 子 span 的 spanLevel 等于父节点 + 1，parentSpanId 指向父 span。
     */
    @Test
    void childSpanIncrementsSpanLevel() {
        TraceSnapshot root = TraceSnapshot.startNew();
        TraceSnapshot child = root.childSpan();
        assertEquals(2, child.spanLevel());
        assertEquals(root.spanId(), child.parentSpanId());
        assertEquals(root.traceId(), child.traceId());
    }

    /**
     * 连续 childSpan 逐层 +1，形成 1 → 2 → 3。
     */
    @Test
    void nestedChildSpanIncrementsEachLevel() {
        TraceSnapshot root = TraceSnapshot.startNew();
        TraceSnapshot child = root.childSpan();
        TraceSnapshot grandChild = child.childSpan();
        assertEquals(1, root.spanLevel());
        assertEquals(2, child.spanLevel());
        assertEquals(3, grandChild.spanLevel());
        assertEquals(child.spanId(), grandChild.parentSpanId());
    }

    /**
     * 从 inbound traceparent 续链视作子节点：远程父 span 未知深度时视为根，本段 spanLevel=2。
     */
    @Test
    void continueFromTraceParentUsesChildSpanLevel() {
        String incoming = TraceparentCodec.format(TRACE_ID, SPAN_ID, true);
        TraceSnapshot snapshot = TraceSnapshot.continueFromTraceParent(incoming);
        assertEquals(2, snapshot.spanLevel());
        assertEquals(SPAN_ID, snapshot.parentSpanId());
    }

    /**
     * 无 parent 的显式组装为根，spanLevel=1。
     */
    @Test
    void ofWithoutParentUsesRootSpanLevel() {
        TraceSnapshot snapshot = TraceSnapshot.of(TRACE_ID, SPAN_ID, null, true);
        assertEquals(1, snapshot.spanLevel());
        assertNull(snapshot.parentSpanId());
    }

    /**
     * 带 parent 的四参组装无法得知真实深度，按父为根处理，spanLevel=2。
     */
    @Test
    void ofWithParentInfersChildSpanLevel() {
        TraceSnapshot snapshot = TraceSnapshot.of(TRACE_ID, SPAN_ID, PARENT_SPAN_ID, true);
        assertEquals(2, snapshot.spanLevel());
        assertEquals(PARENT_SPAN_ID, snapshot.parentSpanId());
    }

    /**
     * 显式 spanLevel 可还原深层节点；withSw8 / withBaggage 不得改层级。
     */
    @Test
    void ofExplicitSpanLevelPreservedByWithers() {
        TraceSnapshot snapshot = TraceSnapshot.of(TRACE_ID, SPAN_ID, PARENT_SPAN_ID, true, 4)
                .withSw8("1-sw8")
                .withBaggage(Map.of("k", "v"));
        assertEquals(4, snapshot.spanLevel());
        assertEquals("1-sw8", snapshot.sw8());
        assertEquals("v", snapshot.baggage().get("k"));
    }

    /**
     * 根节点不得声明 spanLevel≠1；子节点不得声明 spanLevel&lt;2。
     */
    @Test
    void ofRejectsInconsistentSpanLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> TraceSnapshot.of(TRACE_ID, SPAN_ID, null, true, 2));
        assertThrows(IllegalArgumentException.class,
                () -> TraceSnapshot.of(TRACE_ID, SPAN_ID, PARENT_SPAN_ID, true, 1));
        assertThrows(IllegalArgumentException.class,
                () -> TraceSnapshot.of(TRACE_ID, SPAN_ID, PARENT_SPAN_ID, true, 0));
    }
}
