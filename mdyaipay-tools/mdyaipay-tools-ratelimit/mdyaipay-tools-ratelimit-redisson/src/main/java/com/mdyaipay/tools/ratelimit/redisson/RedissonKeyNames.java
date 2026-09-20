package com.mdyaipay.tools.ratelimit.redisson;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;

import java.util.Objects;

/**
 * Redisson 限流键命名：与 {@code ratelimit-redis} 相同前缀，便于驱动切换。
 * <p>
 * <b>不负责</b> 逻辑键拼装——由调用方 / KeyResolver 完成。
 */
public final class RedissonKeyNames {

    private static final String PREFIX = "mdyaipay:rl:";

    private RedissonKeyNames() {
    }

    /**
     * 拼装分布式限流 Redis key。
     * <p>
     * 前置条件：{@code algorithm} 非 null；{@code logicalKey} 非 blank。
     * 幂等：是。
     *
     * @param algorithm  算法枚举
     * @param logicalKey 业务逻辑键
     * @return Redis key
     */
    public static String of(RateLimitAlgorithm algorithm, String logicalKey) {
        Objects.requireNonNull(algorithm, "algorithm");
        if (logicalKey == null || logicalKey.isBlank()) {
            throw new IllegalArgumentException("logicalKey must not be blank");
        }
        return PREFIX + algorithm.name() + ':' + logicalKey;
    }
}
