package com.mdyaipay.tools.ratelimit.redisson;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redisson 滑动窗口日志集成测。
 */
class RedissonSlidingWindowLogRateLimiterIT {

    /**
     * 窗口内达到 limit 后拒绝；推进窗口后恢复。
     */
    @Test
    void zsetSlidingWindowAllowsThenRejects() throws InterruptedException {
        RedissonClient client = RedissonIntegrationSupport.openOrSkip();
        try {
            RedissonSlidingWindowLogRateLimiter limiter = RedissonSlidingWindowLogRateLimiter.wrap(client);
            RateLimitPolicy policy = new RateLimitPolicy(
                    RateLimitAlgorithm.SLIDING_WINDOW_LOG, 3, Duration.ofSeconds(1), 0, 0);
            String key = "swlog-redisson-" + UUID.randomUUID();
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            RateLimitDecision denied = limiter.tryAcquire(key, policy);
            assertFalse(denied.allowed());
            assertEquals(0, denied.remaining());
            assertEquals(RateLimitAlgorithm.SLIDING_WINDOW_LOG, denied.algorithm());

            Thread.sleep(1100L);
            assertTrue(limiter.tryAcquire(key, policy).allowed());
        } finally {
            client.shutdown();
        }
    }
}
