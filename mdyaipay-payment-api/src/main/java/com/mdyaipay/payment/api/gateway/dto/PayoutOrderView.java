package com.mdyaipay.payment.api.gateway.dto;

import java.io.Serializable;
import java.time.Instant;

/** 代付单视图；{@code amount} 为分。 */
public final class PayoutOrderView implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String payoutNo;
    private final long amount;
    private final String channel;
    private final String payeeRef;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public PayoutOrderView(
            String payoutNo,
            long amount,
            String channel,
            String payeeRef,
            String status,
            Instant createdAt,
            Instant updatedAt) {
        this.payoutNo = payoutNo;
        this.amount = amount;
        this.channel = channel;
        this.payeeRef = payeeRef;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getPayoutNo() {
        return payoutNo;
    }

    public long getAmount() {
        return amount;
    }

    public String getChannel() {
        return channel;
    }

    public String getPayeeRef() {
        return payeeRef;
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
