package com.mdyaipay.tools.trace.dubbo;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceHeaders;
import com.mdyaipay.tools.trace.TraceSnapshot;
import com.mdyaipay.tools.trace.autoconfigure.TraceProperties;
import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Dubbo Consumer/Provider Filter 的 Trace 注入与恢复行为。
 */
class TraceDubboFilterTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    /**
     * Consumer 侧应将当前 TraceContext 写入 invocation attachment。
     */
    @Test
    void consumerFilterInjectsTraceParentWhenContextPresent() throws RpcException {
        TraceSnapshot root = TraceSnapshot.startNew();
        TraceContext.bind(root);

        Map<String, String> attachments = new HashMap<>();
        Invocation invocation = mock(Invocation.class);
        when(invocation.getAttachment(any())).thenAnswer(inv -> attachments.get(inv.getArgument(0)));
        doAnswer(inv -> {
            attachments.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(invocation).setAttachment(any(), any());

        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(invocation)).thenReturn(mock(Result.class));

        new TraceDubboConsumerFilter().invoke(invoker, invocation);

        assertTrue(attachments.containsKey(TraceHeaders.TRACE_PARENT));
        assertEquals(root.toTraceParentHeader(), attachments.get(TraceHeaders.TRACE_PARENT));
    }

    /**
     * Provider 侧应从 attachment 续链并绑定 TraceContext，invoke 结束后清理。
     */
    @Test
    void providerFilterExtractsAndBindsTrace() throws RpcException {
        TraceSnapshot upstream = TraceSnapshot.startNew();
        Map<String, String> attachments = Map.of(
                TraceHeaders.TRACE_PARENT, upstream.toTraceParentHeader());

        Invocation invocation = mock(Invocation.class);
        when(invocation.getAttachment(TraceHeaders.TRACE_PARENT))
                .thenReturn(attachments.get(TraceHeaders.TRACE_PARENT));
        when(invocation.getAttachment(TraceHeaders.TRACE_STATE)).thenReturn(null);
        when(invocation.getAttachment(TraceHeaders.SW8)).thenReturn(null);

        AtomicReference<String> traceIdInInvoke = new AtomicReference<>();
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(invocation)).thenAnswer(inv -> {
            traceIdInInvoke.set(TraceContext.currentTraceId().orElse(null));
            return mock(Result.class);
        });

        TraceMdcSupport mdcSupport = new TraceMdcSupport(new TraceProperties());
        new TraceDubboProviderFilter(mdcSupport).invoke(invoker, invocation);

        assertEquals(upstream.traceId(), traceIdInInvoke.get());
        assertTrue(TraceContext.current().isEmpty());
    }

    /**
     * Provider 续链后 spanId 须为本段新值，parent 指向上游 span。
     */
    @Test
    void providerFilterCreatesChildSpanFromAttachment() throws RpcException {
        TraceSnapshot upstream = TraceSnapshot.startNew();
        Invocation invocation = mock(Invocation.class);
        when(invocation.getAttachment(TraceHeaders.TRACE_PARENT)).thenReturn(upstream.toTraceParentHeader());
        when(invocation.getAttachment(TraceHeaders.TRACE_STATE)).thenReturn(null);
        when(invocation.getAttachment(TraceHeaders.SW8)).thenReturn(null);

        AtomicReference<TraceSnapshot> snapshotInInvoke = new AtomicReference<>();
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(invocation)).thenAnswer(inv -> {
            snapshotInInvoke.set(TraceContext.current().orElseThrow());
            return mock(Result.class);
        });

        new TraceDubboProviderFilter(new TraceMdcSupport(new TraceProperties())).invoke(invoker, invocation);

        TraceSnapshot bound = snapshotInInvoke.get();
        assertEquals(upstream.traceId(), bound.traceId());
        assertEquals(upstream.spanId(), bound.parentSpanId());
        assertNotEquals(upstream.spanId(), bound.spanId());
        assertEquals(1, bound.spanLevel());
        assertEquals(2, bound.serverDepthLevel());
        assertEquals(2, bound.spanLevelGlobal());
    }
}
