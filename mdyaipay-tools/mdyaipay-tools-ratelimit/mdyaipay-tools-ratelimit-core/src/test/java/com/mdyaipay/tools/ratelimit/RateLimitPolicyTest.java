package com.mdyaipay.tools.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link RateLimitPolicy} 构造校验单测。
 */
class RateLimitPolicyTest {

    /**
     * 验证 limit 非正时抛出 IllegalArgumentException。
     */
    @Test
    void rejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.FIXED_WINDOW, 0, Duration.ofSeconds(1), 0, 0));
    }

    /**
     * 验证 window 为 null 或非正时抛出 IllegalArgumentException。
     */
    @Test
    void rejectsNullOrNonPositiveWindow() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.FIXED_WINDOW, 10, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.FIXED_WINDOW, 10, Duration.ZERO, 0, 0));
    }

    /**
     * 验证令牌桶要求 refillRatePerSecond &gt; 0。
     */
    @Test
    void tokenBucketRequiresPositiveRefillRate() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.TOKEN_BUCKET, 10, Duration.ofSeconds(1), 0, 0));
    }

    /**
     * 验证滑动窗口计数要求 slidingSegments ≥ 2。
     */
    @Test
    void slidingWindowRequiresAtLeastTwoSegments() {
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy(RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 10, Duration.ofSeconds(1), 0, 1));
    }

    /**
     * 验证固定窗口最小合法字段可构造。
     */
    @Test
    void acceptsFixedWindowMinimalFields() {
        RateLimitPolicy p = new RateLimitPolicy(
                RateLimitAlgorithm.FIXED_WINDOW, 100, Duration.ofSeconds(1), 0, 0);
        assertEquals(100, p.limit());
    }
}
