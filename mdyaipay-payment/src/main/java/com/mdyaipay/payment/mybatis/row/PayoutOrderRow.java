package com.mdyaipay.payment.mybatis.row;

import java.time.Instant;

/**
 * 代付表行 DTO，供 Mapper 与仓储映射。
 */
public record PayoutOrderRow(
        String payoutNo,
        long amount,
        String channel,
        String payeeRef,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
