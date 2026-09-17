package com.mdyaipay.user.domain.outbox;

import com.mdyaipay.user.domain.outbox.model.DomainOutboxMessage;

import java.time.Instant;
import java.util.List;

/**
 * 领域 Outbox 仓储：与业务事务同库写入，异步投递 RocketMQ。
 */
public interface DomainOutboxRepository {

    void insert(DomainOutboxMessage message);

    List<DomainOutboxMessage> findPending(int limit);

    void markSent(long outboxId, Instant sentAt);
}
