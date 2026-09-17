package com.mdyaipay.user.controller.merchant;

import com.mdyaipay.tools.model.ApiResponse;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialQuery;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialResult;
import com.mdyaipay.user.service.merchant.MerchantBusinessException;
import com.mdyaipay.user.service.merchant.MerchantOpenApiCredentialService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内网 HTTP：供 JDK 轻量网关解析凭证（与 Dubbo {@link com.mdyaipay.user.api.merchant.gateway.MerchantGatewayFacade} 等价）。
 */
@RestController
@RequestMapping("/internal/v1/open-api/credentials")
public class MerchantGatewayInternalController {

    private final MerchantOpenApiCredentialService credentialService;

    public MerchantGatewayInternalController(MerchantOpenApiCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping("/resolve")
    public ApiResponse<ResolveOpenApiCredentialResult> resolve(@RequestParam("appKey") String appKey) {
        try {
            return ApiResponse.ok(credentialService.resolve(new ResolveOpenApiCredentialQuery(appKey)));
        } catch (MerchantBusinessException ex) {
            return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
        }
    }
}
