package com.mdyaipay.tools.ratelimit.observe;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link RateLimitMetrics} 单测。
 */
class RateLimitMetricsTest {

    /**
     * denied 时 decisions 计数递增。
     */
    @Test
    void incrementsDeniedCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimitMetrics metrics = new RateLimitMetrics(registry);
        metrics.record(
                "gateway-payment-collect",
                RateLimitMetrics.OUTCOME_DENIED,
                "redisson",
                "/api/v1/payments/collect",
                20L,
                1000L,
                null,
                false);
        assertEquals(
                1.0d,
                registry.get("mdyaipay.ratelimit.decisions")
                        .tag("rule_id", "gateway-payment-collect")
                        .tag("outcome", "denied")
                        .tag("backend", "redisson")
                        .counter()
                        .count());
    }
}
