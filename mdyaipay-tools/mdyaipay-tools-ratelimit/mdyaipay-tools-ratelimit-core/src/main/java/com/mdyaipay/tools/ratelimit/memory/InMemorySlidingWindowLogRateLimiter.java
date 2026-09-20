package com.mdyaipay.tools.ratelimit.memory;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存滑动窗口日志：按请求时间戳 deque 模拟 Redis ZSET 语义，不保证跨 JVM。
 * <p>
 * <b>不负责</b> 分布式一致性——生产多实例请用 {@code ratelimit-redis} ZSET Lua。
 */
public final class InMemorySlidingWindowLogRateLimiter implements RateLimiter {

    private final Clock clock;
    private final ConcurrentHashMap<String, LogState> states = new ConcurrentHashMap<>();

    /**
     * 使用系统 UTC 时钟构造。
     */
    public InMemorySlidingWindowLogRateLimiter() {
        this(Clock.systemUTC());
    }

    /**
     * @param clock 判定用时钟，非 null
     */
    public InMemorySlidingWindowLogRateLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        validate(key, policy);
        long nowMs = clock.millis();
        long windowMs = policy.window().toMillis();
        long limit = policy.limit();
        long windowStart = nowMs - windowMs;

        DecisionHolder holder = new DecisionHolder();
        states.compute(key, (k, prev) -> {
            LogState state = prev == null ? new LogState() : prev;
            prune(state.timestamps, windowStart);
            if (state.timestamps.size() >= limit) {
                long oldest = state.timestamps.peekFirst();
                long retryMs = Math.max(1L, oldest + windowMs - nowMs);
                holder.decision = new RateLimitDecision(
                        false,
                        0,
                        limit,
                        Duration.ofMillis(retryMs),
                        RateLimitAlgorithm.SLIDING_WINDOW_LOG);
                return state;
            }
            state.timestamps.addLast(nowMs);
            long remaining = limit - state.timestamps.size();
            holder.decision = new RateLimitDecision(
                    true,
                    remaining,
                    limit,
                    Duration.ZERO,
                    RateLimitAlgorithm.SLIDING_WINDOW_LOG);
            return state;
        });
        return holder.decision;
    }

    /**
     * 剔除窗口外时间戳（score ≤ windowStart）。
     */
    private static void prune(Deque<Long> timestamps, long windowStart) {
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
            timestamps.removeFirst();
        }
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
        if (policy.algorithm() != RateLimitAlgorithm.SLIDING_WINDOW_LOG) {
            throw new IllegalArgumentException("algorithm must be SLIDING_WINDOW_LOG");
        }
    }

    /**
     * 单 key 请求时间戳队列。
     */
    private static final class LogState {
        private final Deque<Long> timestamps = new ArrayDeque<>();
    }

    /**
     * compute 回调内写出判定结果的持有器。
     */
    private static final class DecisionHolder {
        private RateLimitDecision decision;
    }
}
