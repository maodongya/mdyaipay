package com.mdyaipay.tools.ratelimit.redisson;

import org.junit.jupiter.api.Assumptions;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * Redisson 集成测辅助：连接本机 Redis，不可用则跳过。
 */
final class RedissonIntegrationSupport {

    static final String REDIS_URI = System.getenv().getOrDefault(
            "MDYAIPAY_RATELIMIT_REDIS_URI", "redis://127.0.0.1:6379");

    private RedissonIntegrationSupport() {
    }

    /**
     * 创建客户端并 PING；失败则 Assumption 跳过用例。
     *
     * @return 可用客户端（调用方负责 {@link RedissonClient#shutdown()}）
     */
    static RedissonClient openOrSkip() {
        try {
            Config config = new Config();
            config.useSingleServer().setAddress(REDIS_URI);
            RedissonClient client = Redisson.create(config);
            client.getKeys().count();
            return client;
        } catch (RuntimeException ex) {
            Assumptions.assumeTrue(false, "Redis unavailable at " + REDIS_URI + ": " + ex.getMessage());
            return null;
        }
    }
}
