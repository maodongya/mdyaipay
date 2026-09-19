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
 * 内存滑动窗口计数限流：分段加权逼近滑动窗口，不保证跨 JVM。
 * <p>
 * <b>不负责</b> 其它算法——仅接受 {@link RateLimitAlgorithm#SLIDING_WINDOW_COUNTER}。
 */
public final class InMemorySlidingWindowCounterRateLimiter implements RateLimiter {

    private final Clock clock;
    private final ConcurrentHashMap<String, SegmentState> states = new ConcurrentHashMap<>();

    /**
     * 使用系统 UTC 时钟构造。
     */
    public InMemorySlidingWindowCounterRateLimiter() {
        this(Clock.systemUTC());
    }

    /**
     * @param clock 判定用时钟，非 null
     */
    public InMemorySlidingWindowCounterRateLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        validate(key, policy);
        long nowMs = clock.millis();
        int segments = policy.slidingSegments();
        long windowMs = policy.window().toMillis();
        long segmentMs = Math.max(1L, windowMs / segments);
        long limit = policy.limit();
        long segmentIndex = nowMs / segmentMs;
        long elapsedInSegment = nowMs % segmentMs;

        DecisionHolder holder = new DecisionHolder();
        states.compute(key, (k, prev) -> {
            SegmentState state = rollForward(prev, segmentIndex);
            double weight = 1.0d - (elapsedInSegment / (double) segmentMs);
            double estimated = state.currentCount + state.previousCount * weight;
            if (estimated < limit) {
                SegmentState next = new SegmentState(
                        state.segmentIndex, state.currentCount + 1, state.previousCount);
                long remaining = Math.max(0L, limit - (long) Math.ceil(estimated + 1.0d));
                holder.decision = new RateLimitDecision(
                        true,
                        remaining,
                        limit,
                        Duration.ZERO,
                        RateLimitAlgorithm.SLIDING_WINDOW_COUNTER);
                return next;
            }
            long retryMs = Math.max(1L, segmentMs - elapsedInSegment);
            holder.decision = new RateLimitDecision(
                    false,
                    0,
                    limit,
                    Duration.ofMillis(retryMs),
                    RateLimitAlgorithm.SLIDING_WINDOW_COUNTER);
            return state;
        });
        return holder.decision;
    }

    /**
     * 将状态滚动到当前段；跨多段时上一窗计数清零。
     */
    private static SegmentState rollForward(SegmentState prev, long segmentIndex) {
        if (prev == null) {
            return new SegmentState(segmentIndex, 0, 0);
        }
        if (prev.segmentIndex == segmentIndex) {
            return prev;
        }
        long gap = segmentIndex - prev.segmentIndex;
        long previousCount = gap == 1 ? prev.currentCount : 0;
        return new SegmentState(segmentIndex, 0, previousCount);
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
        if (policy.algorithm() != RateLimitAlgorithm.SLIDING_WINDOW_COUNTER) {
            throw new IllegalArgumentException("algorithm must be SLIDING_WINDOW_COUNTER");
        }
    }

    /**
     * 单 key 分段状态。
     */
    private static final class SegmentState {
        private final long segmentIndex;
        private final long currentCount;
        private final long previousCount;

        private SegmentState(long segmentIndex, long currentCount, long previousCount) {
            this.segmentIndex = segmentIndex;
            this.currentCount = currentCount;
            this.previousCount = previousCount;
        }
    }

    /**
     * compute 回调内写出判定结果的持有器。
     */
    private static final class DecisionHolder {
        private RateLimitDecision decision;
    }
}
