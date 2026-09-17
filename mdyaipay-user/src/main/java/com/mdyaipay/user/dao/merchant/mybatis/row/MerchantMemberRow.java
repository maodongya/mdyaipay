package com.mdyaipay.user.dao.merchant.mybatis.row;

import java.time.Instant;

/**
 * 商户成员表行映射。
 */
public record MerchantMemberRow(
        long memberId,
        long merchantId,
        long userId,
        String role,
        Instant createdAt) {
}
