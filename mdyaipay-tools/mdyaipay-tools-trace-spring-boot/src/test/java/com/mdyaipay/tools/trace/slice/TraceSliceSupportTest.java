package com.mdyaipay.tools.trace.slice;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceSnapshot;
import com.mdyaipay.tools.trace.autoconfigure.TraceProperties;
import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link TraceSliceSupport} 单元测试：不依赖 Spring 容器，验证子 span 绑定与恢复。
 */
class TraceSliceSupportTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    /**
     * 有根 Trace 时 proceed 期间应看到新的 spanId，结束后恢复根 span。
     */
    @Test
    void bindsChildSpanForActiveTrace() throws Throwable {
        TraceSnapshot root = TraceSnapshot.startNew();
        TraceContext.bind(root);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        AtomicReference<String> spanDuringProceed = new AtomicReference<>();
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            spanDuringProceed.set(TraceContext.current().map(TraceSnapshot::spanId).orElse(null));
            return "done";
        });

        TraceMdcSupport mdcSupport = new TraceMdcSupport(new TraceProperties());
        Object result = TraceSliceSupport.aroundWithChildSpan(joinPoint, mdcSupport, false);

        assertEquals("done", result);
        assertNotEquals(root.spanId(), spanDuringProceed.get());
        assertEquals(root.spanId(), TraceContext.current().map(TraceSnapshot::spanId).orElse(null));
    }

    /**
     * 无 Trace 上下文时不绑定，直接 proceed。
     */
    @Test
    void noOpWithoutContext() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(42);

        TraceMdcSupport mdcSupport = new TraceMdcSupport(new TraceProperties());
        assertEquals(42, TraceSliceSupport.aroundWithChildSpan(joinPoint, mdcSupport, false));
        assertTrue(TraceContext.current().isEmpty());
    }
}
