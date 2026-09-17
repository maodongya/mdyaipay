package com.mdyaipay.user.api.merchant;

import java.io.Serializable;

/** 密钥签发结果：{@code appSecret} 仅此一次明文返回。 */
public final class IssueApiCredentialResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String appKey;
    private final String appSecret;

    public IssueApiCredentialResult(String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }
}
