package com.mdyaipay.payment.mybatis.row;

import java.time.Instant;

/**
 * 收单表行 DTO，供 Mapper 与仓储映射。
 */
public record PaymentOrderRow(
        String orderNo,
        Long merchantId,
        long amount,
        String channel,
        String productType,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
