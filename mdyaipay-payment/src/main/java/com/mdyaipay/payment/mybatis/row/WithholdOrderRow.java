package com.mdyaipay.payment.mybatis.row;

import java.time.Instant;

public record WithholdOrderRow(
        String deductionNo,
        String agreementNo,
        long amount,
        String channel,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
