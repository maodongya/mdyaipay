package com.mdyaipay.tools.ratelimit.redis;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RedisRateLimiter} 路由与并发不超卖集成测。
 */
class RedisRateLimiterIT {

    /**
     * 三算法各一次成功路径。
     */
    @Test
    void routesByAlgorithm() {
        StatefulRedisConnection<String, String> conn = RedisIntegrationSupport.openOrSkip();
        try (conn; RedisRateLimiter limiter = RedisRateLimiter.wrap(conn)) {
            String suffix = UUID.randomUUID().toString();
            assertTrue(limiter.tryAcquire("r-fw-" + suffix, new RateLimitPolicy(
                    RateLimitAlgorithm.FIXED_WINDOW, 1, Duration.ofSeconds(1), 0, 0)).allowed());
            assertTrue(limiter.tryAcquire("r-sw-" + suffix, new RateLimitPolicy(
                    RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, 1, Duration.ofSeconds(1), 0, 2)).allowed());
            assertTrue(limiter.tryAcquire("r-tb-" + suffix, new RateLimitPolicy(
                    RateLimitAlgorithm.TOKEN_BUCKET, 1, Duration.ofSeconds(1), 10.0, 0)).allowed());
        }
    }

    /**
     * blank key 抛 IllegalArgumentException。
     */
    @Test
    void rejectsBlankKey() {
        StatefulRedisConnection<String, String> conn = RedisIntegrationSupport.openOrSkip();
        try (conn; RedisRateLimiter limiter = RedisRateLimiter.wrap(conn)) {
            RateLimitPolicy policy = new RateLimitPolicy(
                    RateLimitAlgorithm.FIXED_WINDOW, 1, Duration.ofSeconds(1), 0, 0);
            assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(" ", policy));
        }
    }

    /**
     * 32 线程并发固定窗口不超卖。
     */
    @Test
    void thirtyTwoThreadsDoNotOversellFixedWindow() throws Exception {
        StatefulRedisConnection<String, String> conn = RedisIntegrationSupport.openOrSkip();
        try (conn; RedisRateLimiter limiter = RedisRateLimiter.wrap(conn)) {
            int limit = 100;
            RateLimitPolicy policy = new RateLimitPolicy(
                    RateLimitAlgorithm.FIXED_WINDOW, limit, Duration.ofSeconds(60), 0, 0);
            String key = "conc-fw-" + UUID.randomUUID();
            AtomicInteger allowed = new AtomicInteger();
            int threads = 32;
            int attemptsPerThread = 20;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < attemptsPerThread; i++) {
                            if (limiter.tryAcquire(key, policy).allowed()) {
                                allowed.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS));
            pool.shutdownNow();
            assertEquals(limit, allowed.get());
        }
    }
}
