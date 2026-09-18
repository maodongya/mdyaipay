package com.mdyaipay.payment.repository.mybatis.row;

import java.time.Instant;

/**
 * 收单表行 DTO，供 Mapper 与仓储映射。
 * <p>{@code id} 为 UUIDv7 的 {@code BINARY(16)} 字节。</p>
 */
public record PaymentOrderRow(
        byte[] id,
        String orderNo,
        Long merchantId,
        long amount,
        String channel,
        String productType,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
