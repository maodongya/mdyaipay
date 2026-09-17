package com.mdyaipay.payment.mybatis.row;

import java.time.Instant;

/**
 * 代扣表行 DTO，供 Mapper 与仓储映射。
 */
public record WithholdOrderRow(
        String deductionNo,
        String agreementNo,
        long amount,
        String channel,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
