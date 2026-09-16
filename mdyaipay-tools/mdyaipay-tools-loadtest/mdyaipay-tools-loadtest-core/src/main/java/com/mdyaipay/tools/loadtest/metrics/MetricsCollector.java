package com.mdyaipay.tools.loadtest.metrics;

import com.mdyaipay.tools.loadtest.model.MetricsProfile;
import com.mdyaipay.tools.loadtest.model.SampleOutcome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * 线程安全的正式阶段指标聚合。
 * <p>
 * 延迟分位数基于 {@link LatencyReservoir} 的有限样本池（近似值，见设计文档）。
 */
public final class MetricsCollector {

    private final MetricsProfile profile;
    private final LatencyReservoir reservoir;
    private final LongAdder total = new LongAdder();
    private final LongAdder success = new LongAdder();
    private final LongAdder errors = new LongAdder();
    private final List<String> errorSamples = new ArrayList<>();

    public MetricsCollector(MetricsProfile profile) {
        this.profile = profile;
        this.reservoir = new LatencyReservoir(LatencyReservoir.DEFAULT_MAX_SAMPLES);
    }

    public void record(SampleOutcome outcome) {
        total.increment();
        if (outcome.success()) {
            success.increment();
        } else {
            errors.increment();
            maybeRecordError(outcome);
        }
        reservoir.record(outcome.latencyNanos());
    }

    private void maybeRecordError(SampleOutcome outcome) {
        if (!profile.recordErrors()) {
            return;
        }
        synchronized (errorSamples) {
            if (errorSamples.size() >= profile.maxErrorSamples()) {
                return;
            }
            String msg = outcome.errorMessage() != null ? outcome.errorMessage() : "unknown error";
            errorSamples.add("status=" + outcome.statusCode() + " " + msg);
        }
    }

    public long totalSamples() {
        return total.sum();
    }

    public long successCount() {
        return success.sum();
    }

    public long errorCount() {
        return errors.sum();
    }

    public List<String> errorSamples() {
        synchronized (errorSamples) {
            return List.copyOf(errorSamples);
        }
    }

    public Map<Double, Double> latencyPercentilesMillis() {
        LatencyReservoir.Snapshot snap = reservoir.snapshot();
        Map<Double, Double> map = new LinkedHashMap<>();
        for (Double p : profile.percentiles()) {
            map.put(p, snap.percentile(p));
        }
        return map;
    }

    public double minLatencyMillis() {
        return reservoir.snapshot().minMillis();
    }

    public double maxLatencyMillis() {
        return reservoir.snapshot().maxMillis();
    }

    public double meanLatencyMillis() {
        return reservoir.snapshot().meanMillis();
    }
}
