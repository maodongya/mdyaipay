package com.mdyaipay.tools.ratelimit.memory;

import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;

import java.time.Clock;
import java.util.Objects;

/**
 * 内存限流门面：按 {@link RateLimitPolicy#algorithm()} 委托给对应算法实现。
 * <p>
 * <b>不负责</b> 跨 JVM 一致性——仅单机 / 单测；生产多实例请用 Redis 驱动。
 */
public final class InMemoryRateLimiter implements RateLimiter {

    private final InMemoryFixedWindowRateLimiter fixedWindow;
    private final InMemorySlidingWindowCounterRateLimiter slidingWindow;
    private final InMemoryTokenBucketRateLimiter tokenBucket;

    /**
     * 使用系统 UTC 时钟构造三算法委托。
     */
    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    /**
     * @param clock 共享时钟，非 null；三算法共用同一时钟便于单测推进时间
     */
    public InMemoryRateLimiter(Clock clock) {
        Clock c = Objects.requireNonNull(clock, "clock");
        this.fixedWindow = new InMemoryFixedWindowRateLimiter(c);
        this.slidingWindow = new InMemorySlidingWindowCounterRateLimiter(c);
        this.tokenBucket = new InMemoryTokenBucketRateLimiter(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        return switch (policy.algorithm()) {
            case FIXED_WINDOW -> fixedWindow.tryAcquire(key, policy);
            case SLIDING_WINDOW_COUNTER -> slidingWindow.tryAcquire(key, policy);
            case TOKEN_BUCKET -> tokenBucket.tryAcquire(key, policy);
        };
    }
}
