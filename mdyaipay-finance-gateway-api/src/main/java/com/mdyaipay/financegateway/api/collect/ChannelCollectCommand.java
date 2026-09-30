package com.mdyaipay.financegateway.api.collect;

import java.io.Serializable;

/**
 * 收单渠道提交指令（不含订单状态机，仅渠道所需字段）。
 */
public final class ChannelCollectCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private long amount;
    private String channel;
    /** 收单产品类型名，如 QUICK_COLLECTION、ONLINE_BANKING。 */
    private String productType;
    private Long merchantId;

    public ChannelCollectCommand() {
    }

    public ChannelCollectCommand(
            String orderNo, long amount, String channel, String productType, Long merchantId) {
        this.orderNo = orderNo;
        this.amount = amount;
        this.channel = channel;
        this.productType = productType;
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

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }
}
