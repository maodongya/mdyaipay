package com.mdyaipay.tools.ratelimit.memory;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link InMemorySlidingWindowCounterRateLimiter} 分段计数与窗口恢复单测。
 */
class InMemorySlidingWindowCounterRateLimiterTest {

    private static final RateLimitPolicy POLICY = new RateLimitPolicy(
            RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 5, Duration.ofSeconds(1), 0, 5);

    /**
     * 同段内达到 limit 后拒绝。
     */
    @Test
    void allowsWithinWeightedEstimateThenRejects() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemorySlidingWindowCounterRateLimiter limiter =
                new InMemorySlidingWindowCounterRateLimiter(clock);

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("k", POLICY).allowed(), "i=" + i);
        }
        assertFalse(limiter.tryAcquire("k", POLICY).allowed());
    }

    /**
     * 推进完整 window 后计数衰减，再次允许（硬断言兜底）。
     */
    @Test
    void recoversAfterFullWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemorySlidingWindowCounterRateLimiter limiter =
                new InMemorySlidingWindowCounterRateLimiter(clock);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        }
        assertFalse(limiter.tryAcquire("k", POLICY).allowed());

        clock.advance(Duration.ofSeconds(1));
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
    }

    /**
     * 跨段后上一窗权重衰减，半窗推进后至少恢复 1 次允许（容差断言）。
     */
    @Test
    void previousSegmentWeightDecaysAcrossBoundary() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemorySlidingWindowCounterRateLimiter limiter =
                new InMemorySlidingWindowCounterRateLimiter(clock);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        }
        assertFalse(limiter.tryAcquire("k", POLICY).allowed());

        // 段长 200ms：推进约半窗，上一窗权重下降，应允许至少一次
        clock.advance(Duration.ofMillis(500));
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
    }
}
