package com.mdyaipay.tools.ratelimit;

/**
 * 限流键解析 SPI：从 {@link RateLimitContext} 得到逻辑 key 片段。
 * <p>
 * 返回 {@code null} 表示本解析器不适用，调用方应跳过或换下一解析器。
 * <b>不负责</b> 多解析器组合与冒号拼接——由规则装配层完成。
 */
public interface RateLimitKeyResolver {

    /**
     * 解析限流键；不适用时返回 null。
     * <p>
     * 前置条件：{@code context} 非 null。
     * 幂等：是（纯函数，无副作用）。
     *
     * @param context 请求上下文
     * @return 键片段，或 null 表示跳过
     */
    String resolve(RateLimitContext context);
}
