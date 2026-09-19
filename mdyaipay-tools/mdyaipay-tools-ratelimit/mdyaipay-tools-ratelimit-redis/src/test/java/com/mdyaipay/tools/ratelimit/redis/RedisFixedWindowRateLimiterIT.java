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
 * Redis 固定窗口限流集成测。
 */
class RedisFixedWindowRateLimiterIT {

    /**
     * 同窗口达到 limit 后拒绝。
     */
    @Test
    void allowsUpToLimitThenRejects() {
        StatefulRedisConnection<String, String> conn = RedisIntegrationSupport.openOrSkip();
        try (conn; RedisRateLimiter limiter = RedisRateLimiter.wrap(conn)) {
            RateLimitPolicy policy = new RateLimitPolicy(
                    RateLimitAlgorithm.FIXED_WINDOW, 2, Duration.ofSeconds(1), 0, 0);
            String key = "fw-" + UUID.randomUUID();
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            RateLimitDecision denied = limiter.tryAcquire(key, policy);
            assertFalse(denied.allowed());
            assertEquals(0, denied.remaining());
            assertTrue(denied.retryAfter().toMillis() > 0);
            assertEquals(RateLimitAlgorithm.FIXED_WINDOW, denied.algorithm());
        }
    }
}
