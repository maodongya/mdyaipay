package com.mdyaipay.user.dao.merchant.mybatis.row;

import java.time.Instant;

/**
 * 商户主体表行映射。
 */
public record MerchantRow(
        long merchantId,
        String name,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
