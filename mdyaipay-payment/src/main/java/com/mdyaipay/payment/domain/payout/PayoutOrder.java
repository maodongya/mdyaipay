package com.mdyaipay.payment.domain.payout;

import java.time.Instant;
import java.util.Objects;

/**
 * 代付指令：向收款方账户付款（提现、结算打款等）。
 */
public class PayoutOrder {
    private final String payoutNo;
    private final long amount;
    private final String channel;
    /** 收款方业务侧引用（账号令牌或内部户标识），不落明文卡号 */
    private final String payeeRef;
    private PayoutStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public PayoutOrder(String payoutNo, long amount, String channel, String payeeRef) {
        this(payoutNo, amount, channel, payeeRef, PayoutStatus.CREATED, Instant.now(), Instant.now());
    }

    private PayoutOrder(
            String payoutNo,
            long amount,
            String channel,
            String payeeRef,
            PayoutStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.payoutNo = Objects.requireNonNull(payoutNo, "payoutNo must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.payeeRef = Objects.requireNonNull(payeeRef, "payeeRef must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        this.amount = amount;
    }

    public static PayoutOrder rehydrate(
            String payoutNo,
            long amount,
            String channel,
            String payeeRef,
            PayoutStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new PayoutOrder(payoutNo, amount, channel, payeeRef, status, createdAt, updatedAt);
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

    public PayoutStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing() {
        assertState(PayoutStatus.CREATED);
        this.status = PayoutStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markSuccess() {
        assertState(PayoutStatus.CREATED, PayoutStatus.PROCESSING);
        this.status = PayoutStatus.SUCCESS;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        assertState(PayoutStatus.CREATED, PayoutStatus.PROCESSING);
        this.status = PayoutStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void assertState(PayoutStatus... allowed) {
        for (PayoutStatus allow : allowed) {
            if (allow == this.status) {
                return;
            }
        }
        throw new IllegalStateException("invalid payout status transition from " + status);
    }

    @Override
    public String toString() {
        return "PayoutOrder{" +
                "payoutNo='" + payoutNo + '\'' +
                ", amount=" + amount +
                ", channel='" + channel + '\'' +
                ", payeeRef='" + payeeRef + '\'' +
                ", status=" + status +
                '}';
    }
}
