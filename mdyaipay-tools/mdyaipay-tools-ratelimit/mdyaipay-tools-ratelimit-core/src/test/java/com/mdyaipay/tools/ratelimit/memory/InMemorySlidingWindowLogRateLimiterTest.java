package com.mdyaipay.tools.ratelimit.memory;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link InMemorySlidingWindowLogRateLimiter} 单测。
 */
class InMemorySlidingWindowLogRateLimiterTest {

    /**
     * 同窗口内满额拒绝，推进窗口后允许。
     */
    @Test
    void allowsUpToLimitThenRejectsAndRecovers() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemorySlidingWindowLogRateLimiter limiter = new InMemorySlidingWindowLogRateLimiter(clock);
        RateLimitPolicy policy = new RateLimitPolicy(
                RateLimitAlgorithm.SLIDING_WINDOW_LOG, 2, Duration.ofSeconds(1), 0, 0);

        assertTrue(limiter.tryAcquire("k", policy).allowed());
        assertTrue(limiter.tryAcquire("k", policy).allowed());
        assertFalse(limiter.tryAcquire("k", policy).allowed());

        clock.advance(Duration.ofSeconds(1));
        assertTrue(limiter.tryAcquire("k", policy).allowed());
    }
}
