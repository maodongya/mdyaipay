package com.mdyaipay.tools.trace;

import com.mdyaipay.tools.trace.internal.TraceThreading;

import java.util.Objects;

/**
 * 将 {@link TraceSnapshot} 传播到 worker 线程的 {@link Runnable} 包装。
 */
public final class TraceRunnable implements Runnable {

    private final TraceSnapshot snapshot;
    private final Runnable delegate;

    private TraceRunnable(TraceSnapshot snapshot, Runnable delegate) {
        this.snapshot = TraceThreading.resolveSnapshot(snapshot);
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * 捕获当前上下文子 span 并包装；无上下文时原样返回 delegate。
     */
    public static Runnable wrap(Runnable delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return TraceThreading.resolveSnapshotOrCapture()
                .<Runnable>map(snapshot -> new TraceRunnable(snapshot, delegate))
                .orElse(delegate);
    }

    /** 使用指定快照包装。 */
    public static Runnable wrap(TraceSnapshot snapshot, Runnable delegate) {
        return new TraceRunnable(snapshot, delegate);
    }

    @Override
    public void run() {
        TraceThreading.runWithSnapshot(snapshot, delegate);
    }
}
