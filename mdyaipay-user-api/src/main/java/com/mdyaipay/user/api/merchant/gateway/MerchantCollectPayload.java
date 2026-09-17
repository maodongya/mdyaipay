package com.mdyaipay.user.api.merchant.gateway;

import java.io.Serializable;

/** 解密后的收单明文（JSON 字段 snake_case 与网关解析一致）。 */
public final class MerchantCollectPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private long merchantId;
    private String orderNo;
    private long amount;
    private String channel;
    private String productType;

    public MerchantCollectPayload() {
    }

    public MerchantCollectPayload(
            long merchantId, String orderNo, long amount, String channel, String productType) {
        this.merchantId = merchantId;
        this.orderNo = orderNo;
        this.amount = amount;
        this.channel = channel;
        this.productType = productType;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(long merchantId) {
        this.merchantId = merchantId;
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
}
