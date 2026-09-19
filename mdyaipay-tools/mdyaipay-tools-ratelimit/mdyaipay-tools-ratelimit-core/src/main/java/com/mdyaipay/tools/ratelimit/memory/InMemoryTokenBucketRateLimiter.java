package com.mdyaipay.tools.ratelimit.memory;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存令牌桶限流：满桶突发、按速率补充，不保证跨 JVM。
 * <p>
 * <b>不负责</b> 其它算法——仅接受 {@link RateLimitAlgorithm#TOKEN_BUCKET}。
 * {@link RateLimitPolicy#window()} 仅与 Redis 版字段对齐，本实现不使用。
 */
public final class InMemoryTokenBucketRateLimiter implements RateLimiter {

    private final Clock clock;
    private final ConcurrentHashMap<String, BucketState> states = new ConcurrentHashMap<>();

    /**
     * 使用系统 UTC 时钟构造。
     */
    public InMemoryTokenBucketRateLimiter() {
        this(Clock.systemUTC());
    }

    /**
     * @param clock 判定用时钟，非 null
     */
    public InMemoryTokenBucketRateLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        validate(key, policy);
        long nowMs = clock.millis();
        long capacity = policy.limit();
        double refillRate = policy.refillRatePerSecond();

        DecisionHolder holder = new DecisionHolder();
        states.compute(key, (k, prev) -> {
            BucketState state = prev == null
                    ? new BucketState(capacity, nowMs)
                    : refill(prev, nowMs, capacity, refillRate);
            if (state.tokens >= 1.0d) {
                double left = state.tokens - 1.0d;
                holder.decision = new RateLimitDecision(
                        true,
                        (long) Math.floor(left),
                        capacity,
                        Duration.ZERO,
                        RateLimitAlgorithm.TOKEN_BUCKET);
                return new BucketState(left, nowMs);
            }
            double deficit = 1.0d - state.tokens;
            long retryMs = Math.max(1L, (long) Math.ceil((deficit / refillRate) * 1000.0d));
            holder.decision = new RateLimitDecision(
                    false,
                    0,
                    capacity,
                    Duration.ofMillis(retryMs),
                    RateLimitAlgorithm.TOKEN_BUCKET);
            return state;
        });
        return holder.decision;
    }

    /**
     * 按经过时间补充令牌，不超过桶容量。
     */
    private static BucketState refill(BucketState prev, long nowMs, long capacity, double refillRate) {
        long elapsedMs = Math.max(0L, nowMs - prev.lastRefillMs);
        double added = (elapsedMs / 1000.0d) * refillRate;
        double tokens = Math.min(capacity, prev.tokens + added);
        return new BucketState(tokens, nowMs);
    }

    /**
     * 校验 key / policy / 算法类型。
     */
    private static void validate(String key, RateLimitPolicy policy) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        if (policy.algorithm() != RateLimitAlgorithm.TOKEN_BUCKET) {
            throw new IllegalArgumentException("algorithm must be TOKEN_BUCKET");
        }
    }

    /**
     * 单 key 令牌桶状态。
     */
    private static final class BucketState {
        private final double tokens;
        private final long lastRefillMs;

        private BucketState(double tokens, long lastRefillMs) {
            this.tokens = tokens;
            this.lastRefillMs = lastRefillMs;
        }
    }

    /**
     * compute 回调内写出判定结果的持有器。
     */
    private static final class DecisionHolder {
        private RateLimitDecision decision;
    }
}
