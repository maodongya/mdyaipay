package com.mdyaipay.tools.ratelimit.memory;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link InMemoryTokenBucketRateLimiter} 突发与补充单测。
 */
class InMemoryTokenBucketRateLimiterTest {

    /**
     * 满桶突发至容量后拒绝。
     */
    @Test
    void allowsBurstUpToCapacityThenRejects() {
        RateLimitPolicy policy = new RateLimitPolicy(
                RateLimitAlgorithm.TOKEN_BUCKET, 3, Duration.ofSeconds(1), 1.0, 0);
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemoryTokenBucketRateLimiter limiter = new InMemoryTokenBucketRateLimiter(clock);

        assertTrue(limiter.tryAcquire("k", policy).allowed());
        assertTrue(limiter.tryAcquire("k", policy).allowed());
        assertTrue(limiter.tryAcquire("k", policy).allowed());
        RateLimitDecision d = limiter.tryAcquire("k", policy);
        assertFalse(d.allowed());
        assertEquals(0, d.remaining());
        assertEquals(RateLimitAlgorithm.TOKEN_BUCKET, d.algorithm());
    }

    /**
     * 按 refillRate 随时间补充令牌后再次允许。
     */
    @Test
    void refillsTokensOverTime() {
        RateLimitPolicy policy = new RateLimitPolicy(
                RateLimitAlgorithm.TOKEN_BUCKET, 1, Duration.ofSeconds(1), 1.0, 0);
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemoryTokenBucketRateLimiter limiter = new InMemoryTokenBucketRateLimiter(clock);

        assertTrue(limiter.tryAcquire("k", policy).allowed());
        assertFalse(limiter.tryAcquire("k", policy).allowed());
        clock.advance(Duration.ofSeconds(1));
        assertTrue(limiter.tryAcquire("k", policy).allowed());
    }
}
