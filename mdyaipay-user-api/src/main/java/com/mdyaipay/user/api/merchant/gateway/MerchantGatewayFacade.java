package com.mdyaipay.user.api.merchant.gateway;

import com.mdyaipay.tools.model.ApiResponse;

/**
 * 网关内网 Dubbo：查询商户开放 API 凭证，供解密与验签；不替代商户侧 HMAC 校验逻辑（在 gateway 完成）。
 */
public interface MerchantGatewayFacade {

    ApiResponse<ResolveOpenApiCredentialResult> resolveOpenApiCredential(ResolveOpenApiCredentialQuery query);
}
