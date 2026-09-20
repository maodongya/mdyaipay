package com.mdyaipay.tools.ratelimit.resolve;

import com.mdyaipay.tools.ratelimit.RateLimitContext;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;

/**
 * 从 {@link RateLimitContext#attributes()} 读取 {@code dubboService} 作为限流键片段。
 */
public final class DubboServiceKeyResolver implements RateLimitKeyResolver {

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(RateLimitContext context) {
        if (context == null || context.attributes() == null) {
            return null;
        }
        return context.attributes().get("dubboService");
    }
}
