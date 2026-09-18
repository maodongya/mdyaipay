package com.mdyaipay.tools.trace.internal;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceSnapshot;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 跨线程绑定逻辑，供 {@link com.mdyaipay.tools.trace.TraceRunnable} / {@link TraceCallable} 复用（DRY）。
 */
public final class TraceThreading {

    private TraceThreading() {
    }

    public static TraceSnapshot resolveSnapshot(TraceSnapshot explicit) {
        return Objects.requireNonNull(explicit, "snapshot");
    }

    public static Optional<TraceSnapshot> resolveSnapshotOrCapture() {
        return TraceContext.captureForChildThread();
    }

    public static void runWithSnapshot(TraceSnapshot snapshot, Runnable action) {
        TraceContext.run(snapshot, action);
    }

    public static <V> V callWithSnapshot(TraceSnapshot snapshot, Callable<V> action) throws Exception {
        return TraceContext.run(snapshot, action);
    }
}
