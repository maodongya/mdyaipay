package com.mdyaipay.payment.api.gateway.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;

/** 收单查询/创建结果视图；{@code amount} 为分，{@code status} 为 {@code PaymentStatus} 枚举名。 */
public final class PaymentOrderView implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String orderNo;
    private final Long merchantId;
    private final long amount;
    private final String channel;
    private final String productType;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    @JsonCreator
    public PaymentOrderView(
            @JsonProperty("orderNo") String orderNo,
            @JsonProperty("merchantId") Long merchantId,
            @JsonProperty("amount") long amount,
            @JsonProperty("channel") String channel,
            @JsonProperty("productType") String productType,
            @JsonProperty("status") String status,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt) {
        this.orderNo = orderNo;
        this.merchantId = merchantId;
        this.amount = amount;
        this.channel = channel;
        this.productType = productType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public long getAmount() {
        return amount;
    }

    public String getChannel() {
        return channel;
    }

    public String getProductType() {
        return productType;
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
