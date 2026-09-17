package com.mdyaipay.user.domain.merchant;

import java.time.Instant;
import java.util.Objects;

/** 店铺实体：归属单一 {@code merchantId}，营业状态由聚合方法变更。 */
public final class Shop {

    private final long shopId;
    private final long merchantId;
    private final String shopName;
    private final String category;
    private ShopOperatingStatus operatingStatus;
    private final Instant createdAt;
    private Instant updatedAt;

    public Shop(long shopId, long merchantId, String shopName, String category) {
        this(
                shopId,
                merchantId,
                shopName,
                category,
                ShopOperatingStatus.OPEN,
                Instant.now(),
                Instant.now());
    }

    private Shop(
            long shopId,
            long merchantId,
            String shopName,
            String category,
            ShopOperatingStatus operatingStatus,
            Instant createdAt,
            Instant updatedAt) {
        if (shopId <= 0 || merchantId <= 0) {
            throw new IllegalArgumentException("shopId and merchantId must be positive");
        }
        this.shopId = shopId;
        this.merchantId = merchantId;
        this.shopName = Objects.requireNonNull(shopName, "shopName must not be null");
        this.category = category;
        this.operatingStatus = Objects.requireNonNull(operatingStatus, "operatingStatus must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Shop rehydrate(
            long shopId,
            long merchantId,
            String shopName,
            String category,
            ShopOperatingStatus operatingStatus,
            Instant createdAt,
            Instant updatedAt) {
        return new Shop(shopId, merchantId, shopName, category, operatingStatus, createdAt, updatedAt);
    }

    /** 变更营业状态，仅更新内存字段与 {@code updatedAt}。 */
    public void updateOperatingStatus(ShopOperatingStatus next) {
        this.operatingStatus = Objects.requireNonNull(next, "operatingStatus must not be null");
        this.updatedAt = Instant.now();
    }

    public long getShopId() {
        return shopId;
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

    public ShopOperatingStatus getOperatingStatus() {
        return operatingStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
