package com.mdyaipay.user.api.merchant.gateway;

import com.mdyaipay.tools.merchant.MerchantOpenApiSignatures;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 商户经网关提交的加密收单报文：外层 HMAC 覆盖 {@code payload} 密文字段，明文 JSON 见 {@link MerchantCollectPayload}.
 */
public final class MerchantEncryptedCollectRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String appKey;
    private final long timestampMillis;
    private final String nonce;
    private final String signMethod;
    private final String sign;
    private final String payload;

    public MerchantEncryptedCollectRequest(
            String appKey,
            long timestampMillis,
            String nonce,
            String signMethod,
            String sign,
            String payload) {
        this.appKey = Objects.requireNonNull(appKey, "appKey must not be null");
        this.nonce = Objects.requireNonNull(nonce, "nonce must not be null");
        this.signMethod = Objects.requireNonNull(signMethod, "signMethod must not be null");
        this.sign = Objects.requireNonNull(sign, "sign must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        if (timestampMillis <= 0) {
            throw new IllegalArgumentException("timestampMillis must be positive");
        }
        this.timestampMillis = timestampMillis;
    }

    public Map<String, String> signParameters() {
        Map<String, String> params = new HashMap<>(MerchantOpenApiSignatures.envelopeParams(
                appKey, timestampMillis, nonce, signMethod));
        params.put("payload", payload);
        return params;
    }

    public String getAppKey() {
        return appKey;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public String getNonce() {
        return nonce;
    }

    public String getSignMethod() {
        return signMethod;
    }

    public String getSign() {
        return sign;
    }

    public String getPayload() {
        return payload;
    }
}
