package com.mdyaipay.user.api.merchant;

import java.io.Serializable;
import java.util.Objects;

/** 商户创建店铺：须验签；商户须处于 ENABLED。 */
public final class CreateShopRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MerchantSignEnvelope signature;
    private final long merchantId;
    private final String shopName;
    private final String category;

    public CreateShopRequest(
            MerchantSignEnvelope signature,
            long merchantId,
            String shopName,
            String category) {
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
        if (merchantId <= 0) {
            throw new IllegalArgumentException("merchantId must be positive");
        }
        this.merchantId = merchantId;
        this.shopName = Objects.requireNonNull(shopName, "shopName must not be null");
        this.category = category;
    }

    public MerchantSignEnvelope getSignature() {
        return signature;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public String getShopName() {
        return shopName;
    }

    public String getCategory() {
        return category;
    }
}
