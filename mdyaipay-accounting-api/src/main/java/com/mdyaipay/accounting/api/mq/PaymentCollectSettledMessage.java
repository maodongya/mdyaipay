package com.mdyaipay.accounting.api.mq;

import java.io.Serializable;
import java.time.Instant;

/**
 * 收单支付成功事件：驱动商户待结算入账（幂等键 {@link #orderNo}）。
 */
public final class PaymentCollectSettledMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private long amount;
    private Long merchantId;
    private String channel;
    private String productType;
    private Instant settledAt;

    public PaymentCollectSettledMessage() {
    }

    /**
     * @param orderNo     支付业务单号（全局幂等键）
     * @param amount      金额（分）
     * @param merchantId  商户 ID，可为 null（无商户时不写商户账）
     * @param channel     渠道编码
     * @param productType 收单产品类型名
     * @param settledAt   支付成功时间
     */
    public PaymentCollectSettledMessage(
            String orderNo,
            long amount,
            Long merchantId,
            String channel,
            String productType,
            Instant settledAt) {
        this.orderNo = orderNo;
        this.amount = amount;
        this.merchantId = merchantId;
        this.channel = channel;
        this.productType = productType;
        this.settledAt = settledAt;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(Instant settledAt) {
        this.settledAt = settledAt;
    }
}
