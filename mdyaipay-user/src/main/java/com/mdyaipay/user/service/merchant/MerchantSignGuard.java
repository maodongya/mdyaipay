package com.mdyaipay.user.service.merchant;

import com.mdyaipay.user.api.merchant.MerchantSignEnvelope;
import com.mdyaipay.user.integration.merchant.sign.MerchantSignatureVerifier;
import com.mdyaipay.user.integration.merchant.sign.SignVerifyResult;
import com.mdyaipay.tools.logging.AlertEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Objects;

/**
 * 商户开放 API 统一验签入口：委托 {@link com.mdyaipay.user.integration.merchant.sign.MerchantSignatureVerifier}，
 * 失败时抛 {@link MerchantBusinessException} 供 Dubbo 层映射错误码。
 */
public final class MerchantSignGuard {

    private static final Logger LOG = LogManager.getLogger(MerchantSignGuard.class);

    private final MerchantSignatureVerifier signatureVerifier;

    public MerchantSignGuard(MerchantSignatureVerifier signatureVerifier) {
        this.signatureVerifier = Objects.requireNonNull(signatureVerifier, "signatureVerifier");
    }

    /**
     * 验签并返回密钥绑定的 {@code merchantId}。
     *
     * @throws MerchantBusinessException 验签失败或业务码非 0
     */
    public long verifyAndResolveMerchantId(MerchantSignEnvelope envelope, Map<String, String> businessParams) {
        SignVerifyResult result = signatureVerifier.verify(envelope, businessParams);
        if (!result.isSuccess()) {
            LOG.warn("{} error_code={}", AlertEvents.failure(AlertEvents.MERCHANT_AUTH_REJECTED), result.getErrorCode());
            throw new MerchantBusinessException(result.getErrorCode(), "merchant sign verify failed");
        }
        return result.getMerchantId();
    }
}
