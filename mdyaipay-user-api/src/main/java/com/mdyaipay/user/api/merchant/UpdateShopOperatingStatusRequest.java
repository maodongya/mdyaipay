package com.mdyaipay.user.api.merchant;

import java.io.Serializable;
import java.util.Objects;

/** 更新店铺营业状态：{@code operatingStatus} 为 OPEN 或 CLOSED，须验签。 */
public final class UpdateShopOperatingStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MerchantSignEnvelope signature;
    private final long shopId;
    private final String operatingStatus;

    public UpdateShopOperatingStatusRequest(
            MerchantSignEnvelope signature,
            long shopId,
            String operatingStatus) {
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
        if (shopId <= 0) {
            throw new IllegalArgumentException("shopId must be positive");
        }
        this.shopId = shopId;
        this.operatingStatus = Objects.requireNonNull(operatingStatus, "operatingStatus must not be null");
    }

    public MerchantSignEnvelope getSignature() {
        return signature;
    }

    public long getShopId() {
        return shopId;
    }

    public String getOperatingStatus() {
        return operatingStatus;
    }
}
