package com.mdyaipay.tools.ratelimit.redisson;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Objects;

/**
 * Redisson 滑动窗口日志限流：ZSET + {@code RScript}，与 {@code ratelimit-redis} 语义对齐。
 * <p>
 * <b>不负责</b> 创建或关闭 {@link RedissonClient}——由调用方注入；其它算法请用 redis / 内存驱动。
 */
public final class RedissonSlidingWindowLogRateLimiter implements RateLimiter {

    private final RedissonClient redisson;
    private final RedissonSlidingWindowLogScript slidingLogScript;

    /**
     * @param redisson Redisson 客户端，非 null
     */
    public RedissonSlidingWindowLogRateLimiter(RedissonClient redisson) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.slidingLogScript = new RedissonSlidingWindowLogScript(redisson);
    }

    /**
     * 包装已有客户端的便捷工厂。
     *
     * @param redisson Redisson 客户端
     * @return 限流器
     */
    public static RedissonSlidingWindowLogRateLimiter wrap(RedissonClient redisson) {
        return new RedissonSlidingWindowLogRateLimiter(redisson);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 前置条件：{@code policy.algorithm()} 必须为 {@link RateLimitAlgorithm#SLIDING_WINDOW_LOG}。
     */
    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        validate(key, policy);
        String redisKey = RedissonKeyNames.of(RateLimitAlgorithm.SLIDING_WINDOW_LOG, key);
        RedissonServerTime time = RedissonServerTime.read(redisson);
        long windowMs = policy.window().toMillis();
        RedissonLuaResult result = slidingLogScript.eval(
                redisKey,
                Long.toString(policy.limit()),
                Long.toString(time.epochMillis()),
                Long.toString(time.slidingWindowStart(windowMs)),
                time.zsetMember(),
                Long.toString(windowMs));
        return toDecision(result, policy, time, windowMs);
    }

    /**
     * 将 Lua 四元组转为契约 Decision。
     */
    private static RateLimitDecision toDecision(
            RedissonLuaResult result, RateLimitPolicy policy, RedissonServerTime time, long windowMs) {
        if (result.allowed()) {
            return new RateLimitDecision(
                    true,
                    result.remaining(),
                    result.limit(),
                    Duration.ZERO,
                    RateLimitAlgorithm.SLIDING_WINDOW_LOG);
        }
        long retryMs = time.retryAfterMsFromOldest(result.retryAfterMs(), windowMs);
        return new RateLimitDecision(
                false,
                0,
                result.limit(),
                Duration.ofMillis(retryMs),
                RateLimitAlgorithm.SLIDING_WINDOW_LOG);
    }

    /**
     * 校验 key、policy 与算法类型。
     */
    private static void validate(String key, RateLimitPolicy policy) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        if (policy.algorithm() != RateLimitAlgorithm.SLIDING_WINDOW_LOG) {
            throw new IllegalArgumentException("algorithm must be SLIDING_WINDOW_LOG");
        }
    }
}
