package com.mdyaipay.payment.api.gateway.dto;

import java.io.Serializable;
import java.time.Instant;

/** 代扣单视图；{@code amount} 为分。 */
public final class WithholdOrderView implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String deductionNo;
    private final String agreementNo;
    private final long amount;
    private final String channel;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public WithholdOrderView(
            String deductionNo,
            String agreementNo,
            long amount,
            String channel,
            String status,
            Instant createdAt,
            Instant updatedAt) {
        this.deductionNo = deductionNo;
        this.agreementNo = agreementNo;
        this.amount = amount;
        this.channel = channel;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
