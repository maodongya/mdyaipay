package com.mdyaipay.tools.ratelimit.resolve;

import com.mdyaipay.tools.ratelimit.RateLimitContext;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;

/**
 * 解析网关 routeId 为限流键片段。
 */
public final class RouteIdKeyResolver implements RateLimitKeyResolver {

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(RateLimitContext context) {
        if (context == null) {
            return null;
        }
        String routeId = context.routeId();
        return routeId == null || routeId.isBlank() ? null : routeId;
    }
}
