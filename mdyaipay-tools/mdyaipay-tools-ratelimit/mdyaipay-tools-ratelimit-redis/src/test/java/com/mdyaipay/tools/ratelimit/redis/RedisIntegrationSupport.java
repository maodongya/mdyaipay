package com.mdyaipay.tools.ratelimit.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Assumptions;

/**
 * Redis 集成测辅助：连接本机 redis7，不可用则跳过。
 */
final class RedisIntegrationSupport {

    static final String REDIS_URI = System.getenv().getOrDefault(
            "MDYAIPAY_RATELIMIT_REDIS_URI", "redis://127.0.0.1:6379");

    private RedisIntegrationSupport() {
    }

    /**
     * 打开连接并 PING；失败则 Assumption 跳过用例。
     *
     * @return 可用连接（调用方负责 close）
     */
    static StatefulRedisConnection<String, String> openOrSkip() {
        try {
            RedisClient client = RedisClient.create(REDIS_URI);
            StatefulRedisConnection<String, String> connection = client.connect();
            connection.sync().ping();
            return connection;
        } catch (RuntimeException ex) {
            Assumptions.assumeTrue(false, "Redis unavailable at " + REDIS_URI + ": " + ex.getMessage());
            return null;
        }
    }
}
