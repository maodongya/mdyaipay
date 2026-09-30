package com.mdyaipay.tools.sentinel.observe;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link SentinelLocalMetrics} 单测。
 */
class SentinelLocalMetricsTest {

    /**
     * Counter 标签与命名符合 Prometheus 导出约定。
     */
    @Test
    void recordIncrementsCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SentinelLocalMetrics metrics = new SentinelLocalMetrics(registry);
        metrics.record(
                "local:payment-gateway-facade-provider",
                SentinelLocalMetrics.OUTCOME_BLOCKED,
                SentinelLocalMetrics.CHANNEL_DUBBO_PROVIDER);
        assertEquals(
                1.0,
                registry.get(SentinelLocalMetrics.METRIC_ENTRIES)
                        .tag("resource", "local:payment-gateway-facade-provider")
                        .tag("outcome", "blocked")
                        .tag("channel", "dubbo_provider")
                        .counter()
                        .count());
    }
}
