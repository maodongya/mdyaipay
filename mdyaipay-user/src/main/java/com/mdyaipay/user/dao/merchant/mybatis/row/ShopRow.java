package com.mdyaipay.user.dao.merchant.mybatis.row;

import java.time.Instant;

/**
 * 店铺表行映射。
 */
public record ShopRow(
        long shopId,
        long merchantId,
        String shopName,
        String category,
        String operatingStatus,
        Instant createdAt,
        Instant updatedAt) {
}
