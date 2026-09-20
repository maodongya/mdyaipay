package com.mdyaipay.gateway.payment;

import com.mdyaipay.tools.model.ApiResponse;
import com.mdyaipay.user.api.merchant.gateway.MerchantGatewayFacade;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialQuery;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialResult;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 经 Dubbo 向 user 查询开放 API 凭证（{@code user-base-url} 为空时由配置类选用）。
 */
@Component
public class DubboOpenApiCredentialResolver implements OpenApiCredentialResolver {

    @DubboReference(version = "1.0.0", check = false, protocol = "tri")
    private MerchantGatewayFacade merchantGatewayFacade;

    @Override
    public ResolveOpenApiCredentialResult resolve(String appKey) throws MerchantSignedCollectException {
        ApiResponse<ResolveOpenApiCredentialResult> response = merchantGatewayFacade.resolveOpenApiCredential(
                new ResolveOpenApiCredentialQuery(appKey));
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            String msg = response != null ? response.getMessage() : "user service unavailable";
            throw MerchantSignedCollectException.credentialFailed(msg);
        }
        return response.getData();
    }
}
