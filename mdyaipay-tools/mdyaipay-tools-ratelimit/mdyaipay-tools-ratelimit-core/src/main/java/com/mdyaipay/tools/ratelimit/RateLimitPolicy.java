package com.mdyaipay.tools.ratelimit;

import java.time.Duration;

/**
 * 不可变限流策略：算法、配额、窗口及算法专用参数。
 * <p>
 * <b>不负责</b> 从 YAML 绑定——由 spring-boot 配置层映射为本类型。
 *
 * @param algorithm            算法，非 null
 * @param limit                窗口内最大请求数，或令牌桶容量；必须 &gt; 0
 * @param window               固定/滑动窗口长度；令牌桶为 refill 周期对齐字段；必须为正
 * @param refillRatePerSecond  仅 {@link RateLimitAlgorithm#TOKEN_BUCKET}：每秒补充令牌数，须 &gt; 0；其它算法可为 0
 * @param slidingSegments      仅 {@link RateLimitAlgorithm#SLIDING_WINDOW_COUNTER}：分段数，须 ≥ 2；其它可为 0
 */
public record RateLimitPolicy(
        RateLimitAlgorithm algorithm,
        long limit,
        Duration window,
        double refillRatePerSecond,
        int slidingSegments
) {

    /**
     * 构造不可变限流策略并校验字段语义。
     */
    public RateLimitPolicy {
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm must not be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (algorithm == RateLimitAlgorithm.TOKEN_BUCKET && refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("refillRatePerSecond must be > 0 for TOKEN_BUCKET");
        }
        if (algorithm == RateLimitAlgorithm.SLIDING_WINDOW_COUNTER && slidingSegments < 2) {
            throw new IllegalArgumentException("slidingSegments must be >= 2 for SLIDING_WINDOW_COUNTER");
        }
    }
}
