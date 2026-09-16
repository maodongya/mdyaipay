package com.mdyaipay.tools.loadtest.engine;

/**
 * 全进程共享的恒定 RPS 限制：各虚拟用户在采样前 {@link #acquire()}，按固定间隔放行。
 * <p>
 * 与 {@link com.mdyaipay.tools.loadtest.model.LoadProfile#threads()} 配合：线程过多时仍不会超过 {@code targetRps}。
 */
public final class SimpleRateLimiter {

    private final double permitsPerSecond;
    private long nextPermitNanos;

    public SimpleRateLimiter(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    public void acquire() throws InterruptedException {
        if (permitsPerSecond <= 0) {
            return;
        }
        long intervalNanos = (long) (1_000_000_000L / permitsPerSecond);
        long now = System.nanoTime();
        synchronized (this) {
            if (nextPermitNanos == 0) {
                nextPermitNanos = now;
            }
            long waitUntil = Math.max(now, nextPermitNanos);
            nextPermitNanos = waitUntil + intervalNanos;
            long sleepNanos = waitUntil - now;
            if (sleepNanos > 0) {
                long millis = sleepNanos / 1_000_000L;
                int nanos = (int) (sleepNanos % 1_000_000L);
                Thread.sleep(millis, nanos);
            }
        }
    }
}
