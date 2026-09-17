package com.mdyaipay.user.service.merchant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import com.mdyaipay.user.domain.merchant.Merchant;
import com.mdyaipay.user.domain.outbox.OutboxStatus;
import com.mdyaipay.user.domain.outbox.DomainOutboxRepository;
import com.mdyaipay.user.domain.outbox.model.DomainOutboxMessage;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * 审核通过/拒绝时写入 {@code domain_outbox}，事件类型 {@code AUDIT_APPROVED} / {@code AUDIT_REJECTED}。
 */
@Component
public class DefaultMerchantAuditOutboxRecorder implements MerchantAuditOutboxRecorder {

    private static final String AGGREGATE_MERCHANT = "MERCHANT";

    private final DomainOutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public DefaultMerchantAuditOutboxRecorder(
            DomainOutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator,
            ObjectMapper objectMapper) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public void recordApproved(Merchant merchant, String auditor) {
        insertOutbox(merchant, "AUDIT_APPROVED", auditor, null);
    }

    @Override
    public void recordRejected(Merchant merchant, String auditor, String remark) {
        insertOutbox(merchant, "AUDIT_REJECTED", auditor, remark);
    }

    private void insertOutbox(Merchant merchant, String eventType, String auditor, String remark) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("occurredAt", Instant.now().toString());
        payload.put("merchantId", merchant.getMerchantId());
        payload.put("merchantName", merchant.getName());
        payload.put("status", merchant.getStatus().name());
        payload.put("auditor", auditor);
        if (remark != null) {
            payload.put("remark", remark);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("outbox payload serialize failed", ex);
        }
        outboxRepository.insert(new DomainOutboxMessage(
                idGenerator.nextId(),
                AGGREGATE_MERCHANT,
                merchant.getMerchantId(),
                eventType,
                json,
                OutboxStatus.PENDING,
                Instant.now(),
                null));
    }
}
