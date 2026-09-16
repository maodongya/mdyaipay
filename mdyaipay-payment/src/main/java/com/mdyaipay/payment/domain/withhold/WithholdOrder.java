package com.mdyaipay.payment.domain.withhold;

import java.time.Instant;
import java.util.Objects;

/**
 * 代扣指令：依赖事先签约协议，向付款方账户发起扣款。
 */
public class WithholdOrder {
    private final String deductionNo;
    private final String agreementNo;
    private final long amount;
    private final String channel;
    private WithholdStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public WithholdOrder(String deductionNo, String agreementNo, long amount, String channel) {
        this(deductionNo, agreementNo, amount, channel, WithholdStatus.CREATED, Instant.now(), Instant.now());
    }

    private WithholdOrder(
            String deductionNo,
            String agreementNo,
            long amount,
            String channel,
            WithholdStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.deductionNo = Objects.requireNonNull(deductionNo, "deductionNo must not be null");
        this.agreementNo = Objects.requireNonNull(agreementNo, "agreementNo must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        this.amount = amount;
    }

    public static WithholdOrder rehydrate(
            String deductionNo,
            String agreementNo,
            long amount,
            String channel,
            WithholdStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new WithholdOrder(deductionNo, agreementNo, amount, channel, status, createdAt, updatedAt);
    }

    public String getDeductionNo() {
        return deductionNo;
    }

    public String getAgreementNo() {
        return agreementNo;
    }

    public long getAmount() {
        return amount;
    }

    public String getChannel() {
        return channel;
    }

    public WithholdStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing() {
        assertState(WithholdStatus.CREATED);
        this.status = WithholdStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markSuccess() {
        assertState(WithholdStatus.CREATED, WithholdStatus.PROCESSING);
        this.status = WithholdStatus.SUCCESS;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        assertState(WithholdStatus.CREATED, WithholdStatus.PROCESSING);
        this.status = WithholdStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void assertState(WithholdStatus... allowed) {
        for (WithholdStatus allow : allowed) {
            if (allow == this.status) {
                return;
            }
        }
        throw new IllegalStateException("invalid withhold status transition from " + status);
    }

    @Override
    public String toString() {
        return "WithholdOrder{" +
                "deductionNo='" + deductionNo + '\'' +
                ", agreementNo='" + agreementNo + '\'' +
                ", amount=" + amount +
                ", channel='" + channel + '\'' +
                ", status=" + status +
                '}';
    }
}
