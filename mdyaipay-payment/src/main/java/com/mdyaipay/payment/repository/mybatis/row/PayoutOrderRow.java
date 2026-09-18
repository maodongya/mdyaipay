package com.mdyaipay.payment.repository.mybatis.row;

import java.time.Instant;

/**
 * 出款表行 DTO，供 Mapper 与仓储映射。
 * <p>{@code id} 为 UUIDv7 的 {@code BINARY(16)} 字节。</p>
 */
public record PayoutOrderRow(
        byte[] id,
        String payoutNo,
        long amount,
        String channel,
        String payeeRef,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
