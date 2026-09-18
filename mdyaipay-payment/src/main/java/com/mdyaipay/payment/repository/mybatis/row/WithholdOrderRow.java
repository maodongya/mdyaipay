package com.mdyaipay.payment.repository.mybatis.row;

import java.time.Instant;

/**
 * 代扣表行 DTO，供 Mapper 与仓储映射。
 * <p>{@code id} 为 UUIDv7 的 {@code BINARY(16)} 字节。</p>
 */
public record WithholdOrderRow(
        byte[] id,
        String deductionNo,
        String agreementNo,
        long amount,
        String channel,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
