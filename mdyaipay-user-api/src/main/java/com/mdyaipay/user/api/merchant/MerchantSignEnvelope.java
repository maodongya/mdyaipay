package com.mdyaipay.user.api.merchant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * 商户开放 API 公共签名字段。业务参数字符串化后一并参与 canonical 签名（见用户模块 {@code MerchantSignatureSupport}）。
 */
public final class MerchantSignEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前唯一支持的签名算法，与 {@code MerchantSignatureSupport} 一致。 */
    public static final String SIGN_METHOD_HMAC_SHA256 = "HMAC_SHA256";

    private final String appKey;
    private final long timestampMillis;
    private final String nonce;
    private final String signMethod;
    private final String sign;

    @JsonCreator
    public MerchantSignEnvelope(
            @JsonProperty("appKey") String appKey,
            @JsonProperty("timestampMillis") long timestampMillis,
            @JsonProperty("nonce") String nonce,
            @JsonProperty("signMethod") String signMethod,
            @JsonProperty("sign") String sign) {
        this.appKey = Objects.requireNonNull(appKey, "appKey must not be null");
        this.nonce = Objects.requireNonNull(nonce, "nonce must not be null");
        this.signMethod = Objects.requireNonNull(signMethod, "signMethod must not be null");
        this.sign = Objects.requireNonNull(sign, "sign must not be null");
        if (timestampMillis <= 0) {
            throw new IllegalArgumentException("timestampMillis must be positive");
        }
        this.timestampMillis = timestampMillis;
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
}
