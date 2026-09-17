package com.mdyaipay.user.domain.outbox.model;

import com.mdyaipay.user.domain.outbox.OutboxStatus;

import java.time.Instant;
import java.util.Objects;

/** Outbox 行领域表示：{@code payloadJson} 为已序列化事件体，不含密钥。 */
public final class DomainOutboxMessage {

    private final long outboxId;
    private final String aggregateType;
    private final long aggregateId;
    private final String eventType;
    private final String payloadJson;
    private final OutboxStatus status;
    private final Instant createdAt;
    private final Instant sentAt;

    public DomainOutboxMessage(
            long outboxId,
            String aggregateType,
            long aggregateId,
            String eventType,
            String payloadJson,
            OutboxStatus status,
            Instant createdAt,
            Instant sentAt) {
        if (outboxId <= 0 || aggregateId <= 0) {
            throw new IllegalArgumentException("outboxId and aggregateId must be positive");
        }
        this.outboxId = outboxId;
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType");
        this.aggregateId = aggregateId;
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.sentAt = sentAt;
    }

    public long getOutboxId() {
        return outboxId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public long getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
