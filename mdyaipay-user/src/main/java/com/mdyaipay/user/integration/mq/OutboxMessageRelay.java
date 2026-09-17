package com.mdyaipay.user.integration.mq;

import com.mdyaipay.user.config.UserProperties;
import com.mdyaipay.user.domain.outbox.DomainOutboxRepository;
import com.mdyaipay.user.domain.outbox.model.DomainOutboxMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 扫描 {@code domain_outbox} 中 PENDING 记录，投递至 RocketMQ（Tag 与 {@code event_type} 一致）。
 */
@Component
@ConditionalOnProperty(name = "user.mq.enabled", havingValue = "true")
public class OutboxMessageRelay {

    private static final Logger LOG = LogManager.getLogger(OutboxMessageRelay.class);

    private final DomainOutboxRepository outboxRepository;
    private final UserProperties userProperties;
    private final RocketMQTemplate rocketMqTemplate;

    public OutboxMessageRelay(
            DomainOutboxRepository outboxRepository,
            UserProperties userProperties,
            RocketMQTemplate rocketMqTemplate) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
        this.userProperties = Objects.requireNonNull(userProperties);
        this.rocketMqTemplate = rocketMqTemplate;
    }

    @Scheduled(fixedDelayString = "${user.mq.outbox-relay-interval-ms:5000}")
    public void relayPendingMessages() {
        if (!userProperties.getMq().isEnabled()) {
            return;
        }
        List<DomainOutboxMessage> pending = outboxRepository.findPending(50);
        for (DomainOutboxMessage message : pending) {
            /* 功能块：投递 MQ — Topic 固定，Tag 为事件类型 */
            String destination = userProperties.getMq().getMerchantTopic() + ":" + message.getEventType();
            try {
                rocketMqTemplate.syncSend(
                        destination,
                        MessageBuilder.withPayload(message.getPayloadJson()).build());
                outboxRepository.markSent(message.getOutboxId(), Instant.now());
            } catch (RuntimeException ex) {
                LOG.warn("outbox relay failed outboxId={} dest={}", message.getOutboxId(), destination, ex);
            }
        }
    }
}
