package com.mdyaipay.tools.trace;

import com.mdyaipay.tools.trace.internal.TraceThreading;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 将 {@link TraceSnapshot} 传播到 worker 线程的 {@link Callable} 包装。
 *
 * @param <V> 结果类型
 */
public final class TraceCallable<V> implements Callable<V> {

    private final TraceSnapshot snapshot;
    private final Callable<V> delegate;

    private TraceCallable(TraceSnapshot snapshot, Callable<V> delegate) {
        this.snapshot = TraceThreading.resolveSnapshot(snapshot);
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * 捕获当前上下文子 span 并包装；无上下文时原样返回 delegate。
     */
    public static <V> Callable<V> wrap(Callable<V> delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return TraceThreading.resolveSnapshotOrCapture()
                .<Callable<V>>map(snapshot -> new TraceCallable<>(snapshot, delegate))
                .orElse(delegate);
    }

    public static <V> Callable<V> wrap(TraceSnapshot snapshot, Callable<V> delegate) {
        return new TraceCallable<>(snapshot, delegate);
    }

    @Override
    public V call() throws Exception {
        return TraceThreading.callWithSnapshot(snapshot, delegate);
    }
}
