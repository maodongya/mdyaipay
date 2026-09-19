package com.mdyaipay.tools.ratelimit.redis;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis 滑动窗口计数集成测。
 */
class RedisSlidingWindowCounterRateLimiterIT {

    /**
     * 长分段内连打不超卖（避免 200ms 段边界权重衰减干扰）。
     */
    @Test
    void doesNotOversellWithinLongSegment() {
        StatefulRedisConnection<String, String> conn = RedisIntegrationSupport.openOrSkip();
        try (conn; RedisRateLimiter limiter = RedisRateLimiter.wrap(conn)) {
            // window=60s / segments=2 → 段长 30s，短测不会跨段
            RateLimitPolicy policy = new RateLimitPolicy(
                    RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 5, Duration.ofSeconds(60), 0, 2);
            String key = "sw-" + UUID.randomUUID();
            int allowed = 0;
            for (int i = 0; i < 20; i++) {
                if (limiter.tryAcquire(key, policy).allowed()) {
                    allowed++;
                }
            }
            assertEquals(5, allowed);
        }
    }

    /**
     * 短窗满额后墙钟推进约 1s 恢复。
     */
    @Test
    void recoversAfterFullWindow() throws InterruptedException {
        StatefulRedisConnection<String, String> conn = RedisIntegrationSupport.openOrSkip();
        try (conn; RedisRateLimiter limiter = RedisRateLimiter.wrap(conn)) {
            RateLimitPolicy policy = new RateLimitPolicy(
                    RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 3, Duration.ofSeconds(1), 0, 2);
            String key = "sw-rec-" + UUID.randomUUID();
            int allowed = 0;
            for (int i = 0; i < 10; i++) {
                if (limiter.tryAcquire(key, policy).allowed()) {
                    allowed++;
                }
            }
            assertEquals(3, allowed);
            Thread.sleep(1100L);
            assertTrue(limiter.tryAcquire(key, policy).allowed());
        }
    }
}
