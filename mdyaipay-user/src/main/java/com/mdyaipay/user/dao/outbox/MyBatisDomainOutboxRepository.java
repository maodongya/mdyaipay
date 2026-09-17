package com.mdyaipay.user.dao.outbox;

import com.mdyaipay.user.dao.outbox.mybatis.mapper.DomainOutboxMapper;
import com.mdyaipay.user.dao.outbox.mybatis.row.DomainOutboxRow;
import com.mdyaipay.user.domain.outbox.DomainOutboxRepository;
import com.mdyaipay.user.domain.outbox.OutboxStatus;
import com.mdyaipay.user.domain.outbox.model.DomainOutboxMessage;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Outbox 表：Relay 扫描 PENDING 投递 MQ。 */
@Repository
public class MyBatisDomainOutboxRepository implements DomainOutboxRepository {

    private final DomainOutboxMapper domainOutboxMapper;

    public MyBatisDomainOutboxRepository(DomainOutboxMapper domainOutboxMapper) {
        this.domainOutboxMapper = Objects.requireNonNull(domainOutboxMapper);
    }

    @Override
    public void insert(DomainOutboxMessage message) {
        domainOutboxMapper.insert(new DomainOutboxRow(
                message.getOutboxId(),
                message.getAggregateType(),
                message.getAggregateId(),
                message.getEventType(),
                message.getPayloadJson(),
                message.getStatus().name(),
                message.getCreatedAt(),
                message.getSentAt()));
    }

    @Override
    public List<DomainOutboxMessage> findPending(int limit) {
        return domainOutboxMapper.findPending(limit).stream().map(this::fromRow).toList();
    }

    @Override
    public void markSent(long outboxId, Instant sentAt) {
        domainOutboxMapper.markSent(outboxId, sentAt);
    }

    private DomainOutboxMessage fromRow(DomainOutboxRow row) {
        return new DomainOutboxMessage(
                row.outboxId(),
                row.aggregateType(),
                row.aggregateId(),
                row.eventType(),
                row.payload(),
                OutboxStatus.valueOf(row.status()),
                row.createdAt(),
                row.sentAt());
    }
}
