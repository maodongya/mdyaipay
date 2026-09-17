package com.mdyaipay.user.api.merchant.gateway;

import java.io.Serializable;

/**
 * 网关验签/解密用凭证快照。
 * <p>{@code appSecretPlain} 仅内网 Dubbo 返回，禁止日志输出。</p>
 */
public final class ResolveOpenApiCredentialResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long merchantId;
    private final String appSecretPlain;
    private final long maxSkewSeconds;

    public ResolveOpenApiCredentialResult(long merchantId, String appSecretPlain, long maxSkewSeconds) {
        if (merchantId <= 0) {
            throw new IllegalArgumentException("merchantId must be positive");
        }
        if (appSecretPlain == null || appSecretPlain.isBlank()) {
            throw new IllegalArgumentException("appSecretPlain must not be blank");
        }
        if (maxSkewSeconds <= 0) {
            throw new IllegalArgumentException("maxSkewSeconds must be positive");
        }
        this.merchantId = merchantId;
        this.appSecretPlain = appSecretPlain;
        this.maxSkewSeconds = maxSkewSeconds;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public String getAppSecretPlain() {
        return appSecretPlain;
    }

    public long getMaxSkewSeconds() {
        return maxSkewSeconds;
    }
}
