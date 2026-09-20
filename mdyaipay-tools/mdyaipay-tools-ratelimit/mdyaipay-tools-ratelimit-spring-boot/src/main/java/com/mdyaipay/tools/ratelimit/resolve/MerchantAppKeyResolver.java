package com.mdyaipay.tools.ratelimit.resolve;

import com.mdyaipay.tools.ratelimit.RateLimitContext;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;

/**
 * 解析商户 appKey；不读 HTTP body，仅用 context 已填充字段。
 */
public final class MerchantAppKeyResolver implements RateLimitKeyResolver {

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(RateLimitContext context) {
        if (context == null) {
            return null;
        }
        String appKey = context.merchantAppKey();
        return appKey == null || appKey.isBlank() ? null : appKey;
    }
}
