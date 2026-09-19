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
 * Redis 令牌桶集成测。
 */
class RedisTokenBucketRateLimiterIT {

    /**
     * 满桶突发后拒绝，补充后再次允许。
     */
    @Test
    void burstThenRefill() throws InterruptedException {
        StatefulRedisConnection<String, String> conn = RedisIntegrationSupport.openOrSkip();
        try (conn; RedisRateLimiter limiter = RedisRateLimiter.wrap(conn)) {
            RateLimitPolicy policy = new RateLimitPolicy(
                    RateLimitAlgorithm.TOKEN_BUCKET, 3, Duration.ofSeconds(1), 1.0, 0);
            String key = "tb-" + UUID.randomUUID();
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            assertTrue(limiter.tryAcquire(key, policy).allowed());
            RateLimitDecision denied = limiter.tryAcquire(key, policy);
            assertFalse(denied.allowed());
            assertEquals(0, denied.remaining());

            RateLimitPolicy slow = new RateLimitPolicy(
                    RateLimitAlgorithm.TOKEN_BUCKET, 1, Duration.ofSeconds(1), 1.0, 0);
            String key2 = "tb2-" + UUID.randomUUID();
            assertTrue(limiter.tryAcquire(key2, slow).allowed());
            assertFalse(limiter.tryAcquire(key2, slow).allowed());
            Thread.sleep(1100L);
            assertTrue(limiter.tryAcquire(key2, slow).allowed());
        }
    }
}
