package com.mdyaipay.tools.ratelimit;

import java.time.Duration;

/**
 * 限流判定结果：供 Filter 写 429 与响应头。
 * <p>
 * <b>不负责</b> HTTP 序列化——由 Gateway/Servlet 适配层完成。
 *
 * @param allowed    是否允许本次请求
 * @param remaining  剩余配额（拒绝时通常为 0）
 * @param limit      策略上限（窗口容量或桶容量）
 * @param retryAfter 建议重试间隔；允许时可为 {@link Duration#ZERO}
 * @param algorithm  实际使用的算法
 */
public record RateLimitDecision(
        boolean allowed,
        long remaining,
        long limit,
        Duration retryAfter,
        RateLimitAlgorithm algorithm
) {

    /**
     * 规范化判定结果；{@code retryAfter}/{@code algorithm} 不可为 null。
     */
    public RateLimitDecision {
        if (retryAfter == null) {
            throw new IllegalArgumentException("retryAfter must not be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm must not be null");
        }
        if (remaining < 0) {
            throw new IllegalArgumentException("remaining must be >= 0");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
    }
}
