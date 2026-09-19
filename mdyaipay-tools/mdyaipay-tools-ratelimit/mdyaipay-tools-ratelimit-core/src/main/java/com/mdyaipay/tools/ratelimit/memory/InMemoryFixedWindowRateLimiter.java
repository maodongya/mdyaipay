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
 * 内存固定窗口限流：按对齐窗口起点计数，不保证跨 JVM。
 * <p>
 * <b>不负责</b> 其它算法——仅接受 {@link RateLimitAlgorithm#FIXED_WINDOW}。
 */
public final class InMemoryFixedWindowRateLimiter implements RateLimiter {

    private final Clock clock;
    private final ConcurrentHashMap<String, WindowState> states = new ConcurrentHashMap<>();

    /**
     * 使用系统 UTC 时钟构造。
     */
    public InMemoryFixedWindowRateLimiter() {
        this(Clock.systemUTC());
    }

    /**
     * @param clock 判定用时钟，非 null；单测可注入步进时钟
     */
    public InMemoryFixedWindowRateLimiter(Clock clock) {
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
        long windowStartMs = (nowMs / windowMs) * windowMs;

        /* 功能块：原子更新窗口计数 — ConcurrentHashMap.compute 保证单 key 串行 */
        DecisionHolder holder = new DecisionHolder();
        states.compute(key, (k, prev) -> {
            WindowState state = prev;
            if (state == null || state.windowStartMs != windowStartMs) {
                state = new WindowState(windowStartMs, 0);
            }
            if (state.count < limit) {
                WindowState next = new WindowState(windowStartMs, state.count + 1);
                holder.decision = new RateLimitDecision(
                        true,
                        limit - next.count,
                        limit,
                        Duration.ZERO,
                        RateLimitAlgorithm.FIXED_WINDOW);
                return next;
            }
            long retryMs = Math.max(1L, windowStartMs + windowMs - nowMs);
            holder.decision = new RateLimitDecision(
                    false,
                    0,
                    limit,
                    Duration.ofMillis(retryMs),
                    RateLimitAlgorithm.FIXED_WINDOW);
            return state;
        });
        return holder.decision;
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
        if (policy.algorithm() != RateLimitAlgorithm.FIXED_WINDOW) {
            throw new IllegalArgumentException("algorithm must be FIXED_WINDOW");
        }
    }

    /**
     * 单 key 窗口状态。
     */
    private static final class WindowState {
        private final long windowStartMs;
        private final long count;

        private WindowState(long windowStartMs, long count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }
    }

    /**
     * compute 回调内写出判定结果的持有器。
     */
    private static final class DecisionHolder {
        private RateLimitDecision decision;
    }
}
