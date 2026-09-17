package com.mdyaipay.user.api.merchant;

import java.io.Serializable;
import java.util.Objects;

/** 平台创建商户草稿：初始状态 DRAFT，并绑定 {@code ownerUserId} 为 OWNER。 */
public final class CreateMerchantCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String merchantName;
    private final long ownerUserId;

    public CreateMerchantCommand(String merchantName, long ownerUserId) {
        this.merchantName = Objects.requireNonNull(merchantName, "merchantName must not be null");
        if (ownerUserId <= 0) {
            throw new IllegalArgumentException("ownerUserId must be positive");
        }
        this.ownerUserId = ownerUserId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public long getOwnerUserId() {
        return ownerUserId;
    }
}
