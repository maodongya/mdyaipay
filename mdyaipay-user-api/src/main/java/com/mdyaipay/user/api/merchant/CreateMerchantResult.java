package com.mdyaipay.user.api.merchant;

import java.io.Serializable;

/** 创建商户成功：返回平台分配的 {@code merchantId}。 */
public final class CreateMerchantResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long merchantId;

    public CreateMerchantResult(long merchantId) {
        this.merchantId = merchantId;
    }

    public long getMerchantId() {
        return merchantId;
    }
}
