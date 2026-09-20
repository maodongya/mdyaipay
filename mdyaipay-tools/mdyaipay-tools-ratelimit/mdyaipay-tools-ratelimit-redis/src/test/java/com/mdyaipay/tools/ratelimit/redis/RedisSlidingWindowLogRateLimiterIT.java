package com.mdyaipay.tools.ratelimit.redis;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis 滑动窗口日志（ZSET）集成测。
 */
class RedisSlidingWindowLogRateLimiterIT {

    /**
     * 窗口内达到 limit 后拒绝；推进窗口后恢复。
     */
    @Test
    void zsetSlidingWindowAllowsThenRejects() throws InterruptedException {
        StatefulRedisConnection<String, String> conn = RedisIntegrationSupport.openOrSkip();
        try (conn; RedisRateLimiter limiter = RedisRateLimiter.wrap(conn)) {
            RateLimitPolicy policy = new RateLimitPolicy(
                    RateLimitAlgorithm.SLIDING_WINDOW_LOG, 3, Duration.ofSeconds(1), 0, 0);
            String key = "swlog-" + UUID.randomUUID();
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            RateLimitDecision denied = limiter.tryAcquire(key, policy);
            assertFalse(denied.allowed());
            assertEquals(0, denied.remaining());
            assertEquals(RateLimitAlgorithm.SLIDING_WINDOW_LOG, denied.algorithm());

            Thread.sleep(1100L);
            assertTrue(limiter.tryAcquire(key, policy).allowed());
        }
    }
}
