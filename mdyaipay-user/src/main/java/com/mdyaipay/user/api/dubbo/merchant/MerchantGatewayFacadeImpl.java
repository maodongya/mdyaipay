package com.mdyaipay.user.api.dubbo.merchant;

import com.mdyaipay.tools.model.ApiResponse;
import com.mdyaipay.user.api.merchant.gateway.MerchantGatewayFacade;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialQuery;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialResult;
import com.mdyaipay.user.service.merchant.MerchantBusinessException;
import com.mdyaipay.user.service.merchant.MerchantOpenApiCredentialService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/** 网关 Dubbo：解析商户开放 API 凭证。 */
@Component
@DubboService(version = "1.0.0")
public class MerchantGatewayFacadeImpl implements MerchantGatewayFacade {

    private final MerchantOpenApiCredentialService credentialService;

    public MerchantGatewayFacadeImpl(MerchantOpenApiCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    /** 供 gateway 验签解密；失败映射为 {@code ApiResponse}，不向外抛业务异常。 */
    @Override
    public ApiResponse<ResolveOpenApiCredentialResult> resolveOpenApiCredential(ResolveOpenApiCredentialQuery query) {
        try {
            return ApiResponse.ok(credentialService.resolve(query));
        } catch (MerchantBusinessException ex) {
            return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(10002, ex.getMessage());
        }
    }
}
