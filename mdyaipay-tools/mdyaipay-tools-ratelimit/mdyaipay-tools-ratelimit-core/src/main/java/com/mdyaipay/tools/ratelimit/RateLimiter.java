package com.mdyaipay.tools.ratelimit;

/**
 * 限流器端口：一次 {@link #tryAcquire} 对应一次业务请求（或一次 Dubbo 调用）的配额消耗。
 * <p>
 * <b>不负责</b> HTTP/Dubbo 接入与键拼装——由调用方或 Spring Filter 完成。
 */
public interface RateLimiter {

    /**
     * 尝试获取 1 单位配额。
     * <p>
     * 前置条件：{@code key} 非 blank；{@code policy} 非 null 且已通过构造校验。
     * 幂等：否——每次调用可能消耗配额。
     * 副作用：更新该 key 在后端中的计数/令牌状态（内存或 Redis）。
     *
     * @param key    限流维度键（如 {@code merchant:mk_xxx}、{@code route:collect}）
     * @param policy 算法、配额与窗口策略
     * @return 是否允许及剩余配额、建议重试间隔
     */
    RateLimitDecision tryAcquire(String key, RateLimitPolicy policy);
}
