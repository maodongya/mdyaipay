package com.mdyaipay.user.integration.mq;

import com.mdyaipay.user.config.UserProperties;
import com.mdyaipay.user.domain.outbox.DomainOutboxRepository;
import com.mdyaipay.user.domain.outbox.OutboxStatus;
import com.mdyaipay.user.domain.outbox.model.DomainOutboxMessage;
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.logging.LogMessageCapture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Outbox 投递失败事件日志。
 */
class OutboxMessageRelayLoggingTest {

    /** MQ 抛错时必须留下 outbox_relay_failed。 */
    @Test
    void shouldLogOutboxRelayFailed() {
        DomainOutboxMessage pending = new DomainOutboxMessage(
                1L, "merchant", 9L, "MERCHANT_CREATED", "{}", OutboxStatus.PENDING, Instant.now(), null);
        DomainOutboxRepository repository = new DomainOutboxRepository() {
            @Override
            public void insert(DomainOutboxMessage message) {
            }

            @Override
            public List<DomainOutboxMessage> findPending(int limit) {
                return List.of(pending);
            }

            @Override
            public void markSent(long outboxId, Instant sentAt) {
            }
        };
        UserProperties properties = new UserProperties();
        properties.getMq().setEnabled(true);
        OutboxMessageRelay relay = new OutboxMessageRelay(repository, properties, null);

        List<String> messages = LogMessageCapture.capture(OutboxMessageRelay.class, relay::relayPendingMessages);
        assertTrue(messages.stream().anyMatch(line ->
                line.contains(AlertEvents.failure(AlertEvents.OUTBOX_RELAY_FAILED))));
    }
}
