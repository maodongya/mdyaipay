package com.mdyaipay.user.api.merchant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/** 商户提交审核：须验签，且 {@code operatorUserId} 具备 OP 及以上角色。 */
public final class SubmitMerchantAuditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MerchantSignEnvelope signature;
    private final long merchantId;
    private final long operatorUserId;

    @JsonCreator
    public SubmitMerchantAuditRequest(
            @JsonProperty("signature") MerchantSignEnvelope signature,
            @JsonProperty("merchantId") long merchantId,
            @JsonProperty("operatorUserId") long operatorUserId) {
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
        if (merchantId <= 0 || operatorUserId <= 0) {
            throw new IllegalArgumentException("merchantId and operatorUserId must be positive");
        }
        this.merchantId = merchantId;
        this.operatorUserId = operatorUserId;
    }

    public MerchantSignEnvelope getSignature() {
        return signature;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public long getOperatorUserId() {
        return operatorUserId;
    }
}
