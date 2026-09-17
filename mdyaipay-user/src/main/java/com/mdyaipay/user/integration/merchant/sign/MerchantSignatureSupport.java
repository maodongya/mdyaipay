package com.mdyaipay.user.integration.merchant.sign;

import com.mdyaipay.tools.merchant.MerchantOpenApiSignatures;
import com.mdyaipay.user.api.merchant.MerchantSignEnvelope;

import java.util.HashMap;
import java.util.Map;

/**
 * 商户请求签名工具：委托 {@link MerchantOpenApiSignatures}，与用户模块 {@link MerchantSignEnvelope} 字段一致。
 */
public final class MerchantSignatureSupport {

    private MerchantSignatureSupport() {
    }

    public static String buildCanonical(Map<String, String> params) {
        return MerchantOpenApiSignatures.buildCanonical(params);
    }

    public static String signHmacSha256Hex(String secret, String canonical) {
        return MerchantOpenApiSignatures.signHmacSha256Hex(secret, canonical);
    }

    public static Map<String, String> envelopeToParams(MerchantSignEnvelope envelope) {
        Map<String, String> params = new HashMap<>(MerchantOpenApiSignatures.envelopeParams(
                envelope.getAppKey(),
                envelope.getTimestampMillis(),
                envelope.getNonce(),
                envelope.getSignMethod()));
        return params;
    }

    public static boolean constantTimeEquals(String expected, String actual) {
        return MerchantOpenApiSignatures.constantTimeEquals(expected, actual);
    }
}
