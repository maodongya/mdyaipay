package com.mdyaipay.accounting.api.collect;

import java.io.Serializable;
import java.time.Instant;

/**
 * 收单支付成功后的入账指令（幂等键 {@link #orderNo}）。
 */
public final class PaymentCollectSuccessCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private long amount;
    private Long merchantId;
    private String channel;
    private String productType;
    private Instant settledAt;

    public PaymentCollectSuccessCommand() {
    }

    /**
     * @param orderNo     支付业务单号
     * @param amount      金额（分）
     * @param merchantId  商户 ID，无商户时可 null（账务侧跳过入账）
     * @param channel     渠道
     * @param productType 收单产品类型
     * @param settledAt   支付成功时间
     */
    public PaymentCollectSuccessCommand(
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
