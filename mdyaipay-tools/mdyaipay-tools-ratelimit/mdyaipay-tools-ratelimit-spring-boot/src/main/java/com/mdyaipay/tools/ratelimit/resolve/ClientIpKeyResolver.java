package com.mdyaipay.tools.ratelimit.resolve;

import com.mdyaipay.tools.ratelimit.RateLimitContext;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;

/**
 * 解析客户端 IP 为限流键片段。
 */
public final class ClientIpKeyResolver implements RateLimitKeyResolver {

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(RateLimitContext context) {
        if (context == null) {
            return null;
        }
        String ip = context.clientIp();
        return ip == null || ip.isBlank() ? null : ip;
    }
}
