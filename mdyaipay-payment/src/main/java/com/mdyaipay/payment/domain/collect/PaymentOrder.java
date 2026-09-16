package com.mdyaipay.payment.domain.collect;

import java.time.Instant;
import java.util.Objects;

public class PaymentOrder {
    private final String orderNo;
    private final long amount;
    private final String channel;
    private final PaymentProductType productType;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public PaymentOrder(String orderNo, long amount, String channel, PaymentProductType productType) {
        this(orderNo, amount, channel, productType, PaymentStatus.CREATED, Instant.now(), Instant.now());
    }

    private PaymentOrder(
            String orderNo,
            long amount,
            String channel,
            PaymentProductType productType,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.orderNo = Objects.requireNonNull(orderNo, "orderNo must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.productType = Objects.requireNonNull(productType, "productType must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        this.amount = amount;
    }

    /** 从持久化层重建聚合，不触发新建业务校验以外的状态迁移。 */
    public static PaymentOrder rehydrate(
            String orderNo,
            long amount,
            String channel,
            PaymentProductType productType,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new PaymentOrder(orderNo, amount, channel, productType, status, createdAt, updatedAt);
    }

    public String getOrderNo() {
        return orderNo;
    }

    public long getAmount() {
        return amount;
    }

    public String getChannel() {
        return channel;
    }

    public PaymentProductType getProductType() {
        return productType;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing() {
        assertState(PaymentStatus.CREATED);
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markSuccess() {
        assertState(PaymentStatus.CREATED, PaymentStatus.PROCESSING);
        this.status = PaymentStatus.SUCCESS;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        assertState(PaymentStatus.CREATED, PaymentStatus.PROCESSING);
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void assertState(PaymentStatus... allowed) {
        for (PaymentStatus allow : allowed) {
            if (allow == this.status) {
                return;
            }
        }
        throw new IllegalStateException("invalid status transition from " + status);
    }

    @Override
    public String toString() {
        return "PaymentOrder{" +
                "orderNo='" + orderNo + '\'' +
                ", amount=" + amount +
                ", channel='" + channel + '\'' +
                ", productType=" + productType +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
