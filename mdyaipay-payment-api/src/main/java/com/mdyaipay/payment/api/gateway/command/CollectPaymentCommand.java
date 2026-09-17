package com.mdyaipay.payment.api.gateway.command;

import java.io.Serializable;

/** 网关经 Dubbo 发起收单：金额单位为分；{@code productType} 可为空（默认快捷收单）。 */
public final class CollectPaymentCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long merchantId;
    private final String orderNo;
    private final long amount;
    private final String channel;
    private final String productType;

    public CollectPaymentCommand(Long merchantId, String orderNo, long amount, String channel, String productType) {
        this.merchantId = merchantId;
        this.orderNo = orderNo;
        this.amount = amount;
        this.channel = channel;
        this.productType = productType;
    }

    public Long getMerchantId() {
        return merchantId;
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

    public String getProductType() {
        return productType;
    }
}
