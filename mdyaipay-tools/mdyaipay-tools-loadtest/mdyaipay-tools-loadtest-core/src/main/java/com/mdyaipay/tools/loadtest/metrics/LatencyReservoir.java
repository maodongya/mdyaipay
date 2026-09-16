package com.mdyaipay.tools.loadtest.metrics;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 固定上限的延迟样本池：未满时全保留，满后随机替换（ reservoir sampling 简化版）。
 * <p>
 * 包内可见；对外统计由 {@link MetricsCollector} 暴露。
 */
final class LatencyReservoir {

    /** 单轮压测延迟样本上限，避免极高 QPS 时内存膨胀。 */
    static final int DEFAULT_MAX_SAMPLES = 100_000;

    private final long[] samples;
    private final ReentrantLock lock = new ReentrantLock();
    private int size;

    LatencyReservoir(int maxSamples) {
        this.samples = new long[Math.max(1, maxSamples)];
    }

    void record(long latencyNanos) {
        lock.lock();
        try {
            if (size < samples.length) {
                samples[size++] = latencyNanos;
            } else {
                int idx = (int) (Math.random() * samples.length);
                samples[idx] = latencyNanos;
            }
        } finally {
            lock.unlock();
        }
    }

    Snapshot snapshot() {
        lock.lock();
        try {
            long[] copy = Arrays.copyOf(samples, size);
            Arrays.sort(copy);
            return new Snapshot(copy);
        } finally {
            lock.unlock();
        }
    }

    record Snapshot(long[] sortedNanos) {
        double percentile(double p) {
            if (sortedNanos.length == 0) {
                return 0;
            }
            double rank = p * (sortedNanos.length - 1);
            int low = (int) Math.floor(rank);
            int high = (int) Math.ceil(rank);
            if (low == high) {
                return sortedNanos[low] / 1_000_000.0;
            }
            double weight = rank - low;
            double interpolated = sortedNanos[low] * (1 - weight) + sortedNanos[high] * weight;
            return interpolated / 1_000_000.0;
        }

        double minMillis() {
            return sortedNanos.length == 0 ? 0 : sortedNanos[0] / 1_000_000.0;
        }

        double maxMillis() {
            return sortedNanos.length == 0 ? 0 : sortedNanos[sortedNanos.length - 1] / 1_000_000.0;
        }

        double meanMillis() {
            if (sortedNanos.length == 0) {
                return 0;
            }
            long sum = 0;
            for (long n : sortedNanos) {
                sum += n;
            }
            return (sum / (double) sortedNanos.length) / 1_000_000.0;
        }
    }
}
