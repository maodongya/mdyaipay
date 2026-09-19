package com.mdyaipay.tools.ratelimit;

import java.util.Map;

/**
 * 一次请求的限流键解析上下文；字段均可为 null（表示未知）。
 * <p>
 * <b>不负责</b> 从 ServerWebExchange / HttpServletRequest 取值——由 Filter 填充。
 *
 * @param routeId         网关路由 id
 * @param httpMethod      HTTP 方法
 * @param path            请求路径
 * @param clientIp        客户端 IP
 * @param merchantAppKey  商户 appKey
 * @param attributes      扩展属性（不可变拷贝；null 视为空 Map）
 */
public record RateLimitContext(
        String routeId,
        String httpMethod,
        String path,
        String clientIp,
        String merchantAppKey,
        Map<String, String> attributes
) {

    /**
     * 规范化 attributes：null 变为空 Map，否则防御性拷贝。
     */
    public RateLimitContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
