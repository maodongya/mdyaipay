package com.mdyaipay.tools.trace.http;

import com.mdyaipay.tools.trace.TextMapCarrier;
import com.mdyaipay.tools.trace.TraceHeaders;
import com.mdyaipay.tools.trace.TraceSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IncomingTraceResolverTest {

    /**
     * 验证入站 traceparent 被解析为延续 traceId 的新段快照。
     */
    @Test
    void resolvesTraceParentFromCarrier() {
        TraceSnapshot root = TraceSnapshot.startNew();
        TextMapCarrier carrier = TextMapCarrier.create();
        carrier.set(TraceHeaders.TRACE_PARENT, root.toTraceParentHeader());

        TraceSnapshot resolved = IncomingTraceResolver.resolve(carrier);
        assertEquals(root.traceId(), resolved.traceId());
        assertNotNull(resolved.spanId());
    }
}
