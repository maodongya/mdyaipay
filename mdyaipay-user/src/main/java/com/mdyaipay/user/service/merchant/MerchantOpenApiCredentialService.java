package com.mdyaipay.user.service.merchant;

import com.mdyaipay.user.api.UserErrorCodes;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialQuery;
import com.mdyaipay.user.api.merchant.gateway.ResolveOpenApiCredentialResult;
import com.mdyaipay.user.config.merchant.UserMerchantSignConfig;
import com.mdyaipay.user.domain.merchant.Merchant;
import com.mdyaipay.user.domain.merchant.MerchantApiCredential;
import com.mdyaipay.user.domain.merchant.MerchantApiCredentialRepository;
import com.mdyaipay.user.domain.merchant.MerchantRepository;
import org.springframework.stereotype.Service;

/**
 * 网关内网：按 appKey 返回验签/解密所需凭证，不记录 secret。
 */
@Service
public class MerchantOpenApiCredentialService {

    private final MerchantApiCredentialRepository credentialRepository;
    private final MerchantRepository merchantRepository;
    private final UserMerchantSignConfig signConfig;

    public MerchantOpenApiCredentialService(
            MerchantApiCredentialRepository credentialRepository,
            MerchantRepository merchantRepository,
            UserMerchantSignConfig signConfig) {
        this.credentialRepository = credentialRepository;
        this.merchantRepository = merchantRepository;
        this.signConfig = signConfig;
    }

    public ResolveOpenApiCredentialResult resolve(ResolveOpenApiCredentialQuery query) {
        MerchantApiCredential credential = credentialRepository.findByAppKey(query.getAppKey()).orElse(null);
        if (credential == null || !credential.isActive()) {
            throw new MerchantBusinessException(UserErrorCodes.CREDENTIAL_DISABLED, "credential not found or disabled");
        }
        Merchant merchant = merchantRepository.findById(credential.getMerchantId()).orElse(null);
        if (merchant == null || !merchant.allowsMerchantApi()) {
            throw new MerchantBusinessException(UserErrorCodes.MERCHANT_STATE_INVALID, "merchant not allowed for open api");
        }
        return new ResolveOpenApiCredentialResult(
                credential.getMerchantId(),
                credential.getSecretPlain(),
                signConfig.getMaxSkewSeconds());
    }
}
