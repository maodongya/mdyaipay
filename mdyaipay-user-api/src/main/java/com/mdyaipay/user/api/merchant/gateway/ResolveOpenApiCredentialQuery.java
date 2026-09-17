package com.mdyaipay.user.api.merchant.gateway;

import java.io.Serializable;
import java.util.Objects;

/** 网关内网：按 appKey 解析商户开放 API 凭证（无商户签名）。 */
public final class ResolveOpenApiCredentialQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String appKey;

    public ResolveOpenApiCredentialQuery(String appKey) {
        this.appKey = Objects.requireNonNull(appKey, "appKey must not be null");
        if (appKey.isBlank()) {
            throw new IllegalArgumentException("appKey must not be blank");
        }
    }

    public String getAppKey() {
        return appKey;
    }
}
