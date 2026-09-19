package com.mdyaipay.tools.ratelimit;

/**
 * 限流算法枚举：首版仅含固定窗口、滑动窗口计数、令牌桶。
 * <p>
 * <b>不负责</b> P2 扩展（滑动窗口日志、漏桶）——待有明确需求再增枚举值。
 */
public enum RateLimitAlgorithm {

    /** 固定窗口：窗口内计数，边界可能双倍突发。 */
    FIXED_WINDOW,

    /** 滑动窗口计数：分段加权，适合服务接口总量控制。 */
    SLIDING_WINDOW_COUNTER,

    /** 令牌桶：允许合理突发，适合网关入口整形。 */
    TOKEN_BUCKET
}
