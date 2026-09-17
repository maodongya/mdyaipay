package com.mdyaipay.gateway.payment;

import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialResult;

/** 按 appKey 解析商户凭证（Dubbo 或 HTTP 内网）。 */
public interface OpenApiCredentialResolver {

    /**
     * 按 {@code appKey} 解析凭证与时间窗配置。
     *
     * @throws MerchantSignedCollectException 凭证不存在或 user 不可用
     */
    ResolveOpenApiCredentialResult resolve(String appKey) throws MerchantSignedCollectException;
}
