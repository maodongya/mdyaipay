package com.mdyaipay.user.api.merchant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/** 平台为商户签发开放 API 密钥（{@code operator} 记入审计，可选）。 */
public final class IssueApiCredentialCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long merchantId;
    private final String operator;

    @JsonCreator
    public IssueApiCredentialCommand(
            @JsonProperty("merchantId") long merchantId,
            @JsonProperty("operator") String operator) {
        if (merchantId <= 0) {
            throw new IllegalArgumentException("merchantId must be positive");
        }
        this.merchantId = merchantId;
        this.operator = operator;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public String getOperator() {
        return operator;
    }
}
