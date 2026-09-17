package com.mdyaipay.user.dao.outbox.mybatis.row;

import java.time.Instant;

/**
 * 领域 Outbox 表行映射。
 */
public record DomainOutboxRow(
        long outboxId,
        String aggregateType,
        long aggregateId,
        String eventType,
        String payload,
        String status,
        Instant createdAt,
        Instant sentAt) {
}
