package com.mdyaipay.payment.mybatis.row;

import java.time.Instant;

public record PaymentOrderRow(
        String orderNo,
        long amount,
        String channel,
        String productType,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
