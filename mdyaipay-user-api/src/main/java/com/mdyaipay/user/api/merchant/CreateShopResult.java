package com.mdyaipay.user.api.merchant;

import java.io.Serializable;

/** 店铺创建成功：返回 {@code shopId}。 */
public final class CreateShopResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long shopId;

    public CreateShopResult(long shopId) {
        this.shopId = shopId;
    }

    public long getShopId() {
        return shopId;
    }
}
