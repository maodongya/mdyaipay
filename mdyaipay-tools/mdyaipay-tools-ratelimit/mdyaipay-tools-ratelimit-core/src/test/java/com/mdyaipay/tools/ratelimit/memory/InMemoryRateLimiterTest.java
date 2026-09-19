package com.mdyaipay.tools.ratelimit.memory;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link InMemoryRateLimiter} 算法路由单测。
 */
class InMemoryRateLimiterTest {

    /**
     * 按 policy.algorithm 委托到对应内存实现。
     */
    @Test
    void routesByAlgorithm() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);
        assertTrue(limiter.tryAcquire("k", new RateLimitPolicy(
                RateLimitAlgorithm.FIXED_WINDOW, 1, Duration.ofSeconds(1), 0, 0)).allowed());
        assertTrue(limiter.tryAcquire("k2", new RateLimitPolicy(
                RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 1, Duration.ofSeconds(1), 0, 2)).allowed());
        assertTrue(limiter.tryAcquire("k3", new RateLimitPolicy(
                RateLimitAlgorithm.TOKEN_BUCKET, 1, Duration.ofSeconds(1), 10.0, 0)).allowed());
    }

    /**
     * blank key 抛 IllegalArgumentException。
     */
    @Test
    void rejectsBlankKey() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();
        RateLimitPolicy policy = new RateLimitPolicy(
                RateLimitAlgorithm.FIXED_WINDOW, 1, Duration.ofSeconds(1), 0, 0);
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(" ", policy));
    }
}
