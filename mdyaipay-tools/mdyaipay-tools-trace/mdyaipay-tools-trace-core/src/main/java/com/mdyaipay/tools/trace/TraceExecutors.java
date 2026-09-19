package com.mdyaipay.tools.trace;

import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 包装 {@link ExecutorService}，使 {@link #execute} / {@link #submit} 路径自动携带 Trace。
 * <p>
 * 通过覆盖 {@link AbstractExecutorService#execute} 实现，不重复实现 invoke 族逻辑（KISS）。
 */
public final class TraceExecutors {

    private TraceExecutors() {
    }

    /** 包装已有线程池。 */
    public static ExecutorService wrap(ExecutorService delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return new AbstractExecutorService() {
            @Override
            public void execute(Runnable command) {
                delegate.execute(TraceRunnable.wrap(command));
            }

            @Override
            public void shutdown() {
                delegate.shutdown();
            }

            @Override
            public java.util.List<Runnable> shutdownNow() {
                return delegate.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return delegate.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return delegate.isTerminated();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return delegate.awaitTermination(timeout, unit);
            }
        };
    }
}
