package com.mdyaipay.tools.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void nestedRunRestoresPrevious() {
        TraceSnapshot outer = TraceSnapshot.startNew();
        TraceSnapshot inner = TraceSnapshot.startNew();

        TraceContext.run(outer, () -> {
            assertEquals(outer.traceId(), TraceContext.currentTraceId().orElseThrow());
            TraceContext.run(inner, () ->
                    assertEquals(inner.traceId(), TraceContext.currentTraceId().orElseThrow()));
            assertEquals(outer.traceId(), TraceContext.currentTraceId().orElseThrow());
        });

        assertTrue(TraceContext.current().isEmpty());
    }

    @Test
    void wrappedExecutorPropagatesTraceId() throws Exception {
        TraceSnapshot root = TraceSnapshot.startNew();
        TraceContext.bind(root);

        ExecutorService executor = TraceExecutors.wrap(Executors.newSingleThreadExecutor());
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> workerTraceId = new AtomicReference<>();
            executor.execute(() -> {
                workerTraceId.set(TraceContext.currentTraceId().orElse(null));
                latch.countDown();
            });
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(root.traceId(), workerTraceId.get());
        } finally {
            executor.shutdownNow();
        }
    }
}
