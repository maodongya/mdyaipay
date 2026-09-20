package com.mdyaipay.tools.ratelimit;

/**
 * 限流算法枚举：固定窗口、滑动窗口计数/日志、令牌桶。
 * <p>
 * <b>不负责</b> 漏桶等扩展——见设计文档。
 */
public enum RateLimitAlgorithm {

    /** 固定窗口：窗口内计数，边界可能双倍突发。 */
    FIXED_WINDOW,

    /** 滑动窗口计数：分段加权，适合服务接口总量控制。 */
    SLIDING_WINDOW_COUNTER,

    /** 滑动窗口日志：Redis ZSET 存每条请求时间戳，窗口内精确计数。 */
    SLIDING_WINDOW_LOG,

    /** 令牌桶：允许合理突发，适合网关入口整形。 */
    TOKEN_BUCKET
}
