package com.mdyaipay.user.api.merchant;

import java.io.Serializable;
import java.util.Objects;

/** 商户添加成员：角色 OP/FIN，不可 ADD OWNER；同 user 重复添加幂等忽略。 */
public final class AddMerchantMemberRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MerchantSignEnvelope signature;
    private final long merchantId;
    private final long userId;
    private final String role;

    public AddMerchantMemberRequest(
            MerchantSignEnvelope signature,
            long merchantId,
            long userId,
            String role) {
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
        if (merchantId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("merchantId and userId must be positive");
        }
        this.merchantId = merchantId;
        this.userId = userId;
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public MerchantSignEnvelope getSignature() {
        return signature;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
