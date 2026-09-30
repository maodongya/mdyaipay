package com.mdyaipay.finance.api.reconcile;

import java.io.Serializable;

/**
 * 一条对账差异。金额单位为分；单边缺失的金额为 null。
 */
public final class ReconciliationDifferenceView implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String type;
    private final String channelTradeNo;
    private final String orderNo;
    private final Long localAmount;
    private final Long channelAmount;

    /**
     * @param type           差异类型枚举名
     * @param channelTradeNo 渠道交易号
     * @param orderNo        我方单号，渠道单边可空
     * @param localAmount    我方金额（分）
     * @param channelAmount  渠道金额（分）
     */
    public ReconciliationDifferenceView(
            String type, String channelTradeNo, String orderNo, Long localAmount, Long channelAmount) {
        this.type = type;
        this.channelTradeNo = channelTradeNo;
        this.orderNo = orderNo;
        this.localAmount = localAmount;
        this.channelAmount = channelAmount;
    }

    public String getType() {
        return type;
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getLocalAmount() {
        return localAmount;
    }

    public Long getChannelAmount() {
        return channelAmount;
    }
}
