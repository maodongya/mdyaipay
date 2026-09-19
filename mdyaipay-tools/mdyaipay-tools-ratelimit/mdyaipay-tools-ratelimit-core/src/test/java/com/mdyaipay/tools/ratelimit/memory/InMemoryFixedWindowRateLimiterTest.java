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
 * {@link InMemoryFixedWindowRateLimiter} 边界与键隔离单测。
 */
class InMemoryFixedWindowRateLimiterTest {

    private static final RateLimitPolicy POLICY = new RateLimitPolicy(
            RateLimitAlgorithm.FIXED_WINDOW, 2, Duration.ofSeconds(1), 0, 0);

    /**
     * 同窗口内达到 limit 后拒绝，并给出正的 retryAfter。
     */
    @Test
    void allowsUpToLimitThenRejectsInSameWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemoryFixedWindowRateLimiter limiter = new InMemoryFixedWindowRateLimiter(clock);

        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        RateLimitDecision denied = limiter.tryAcquire("k", POLICY);
        assertFalse(denied.allowed());
        assertEquals(0, denied.remaining());
        assertEquals(2, denied.limit());
        assertEquals(RateLimitAlgorithm.FIXED_WINDOW, denied.algorithm());
        assertFalse(denied.retryAfter().isNegative() || denied.retryAfter().isZero());
    }

    /**
     * 窗口滚动后计数重置，再次允许。
     */
    @Test
    void resetsCountWhenWindowElapses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemoryFixedWindowRateLimiter limiter = new InMemoryFixedWindowRateLimiter(clock);
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
        assertFalse(limiter.tryAcquire("k", POLICY).allowed());

        clock.advance(Duration.ofSeconds(1));
        assertTrue(limiter.tryAcquire("k", POLICY).allowed());
    }

    /**
     * 不同 key 配额互不影响。
     */
    @Test
    void isolatesKeys() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-20T00:00:00Z"));
        InMemoryFixedWindowRateLimiter limiter = new InMemoryFixedWindowRateLimiter(clock);
        assertTrue(limiter.tryAcquire("a", POLICY).allowed());
        assertTrue(limiter.tryAcquire("a", POLICY).allowed());
        assertFalse(limiter.tryAcquire("a", POLICY).allowed());
        assertTrue(limiter.tryAcquire("b", POLICY).allowed());
    }
}
