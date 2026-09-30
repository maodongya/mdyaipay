package com.mdyaipay.payment.api.gateway.command;

import java.io.Serializable;

/** 渠道确认收单结果：{@code orderNo} 为商户侧收单单号。 */
public final class ChannelConfirmCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String orderNo;
    private final boolean success;
    private final String channelTradeNo;

    public ChannelConfirmCommand(String orderNo, boolean success) {
        this(orderNo, success, null);
    }

    /**
     * @param channelTradeNo 成功确认时的渠道交易号；失败可为 null
     */
    public ChannelConfirmCommand(String orderNo, boolean success, String channelTradeNo) {
        this.orderNo = orderNo;
        this.success = success;
        this.channelTradeNo = channelTradeNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public boolean isSuccess() {
        return success;
    }

    /** 渠道交易号。 */
    public String getChannelTradeNo() {
        return channelTradeNo;
    }
}
