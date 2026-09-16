package com.mdyaipay.payment.mybatis.row;

import java.time.Instant;

public record PayoutOrderRow(
        String payoutNo,
        long amount,
        String channel,
        String payeeRef,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
