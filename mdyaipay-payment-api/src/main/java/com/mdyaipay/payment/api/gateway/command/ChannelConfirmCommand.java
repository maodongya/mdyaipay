package com.mdyaipay.payment.api.gateway.command;

import java.io.Serializable;

/** 渠道确认收单结果：{@code orderNo} 为商户侧收单单号。 */
public final class ChannelConfirmCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String orderNo;
    private final boolean success;

    public ChannelConfirmCommand(String orderNo, boolean success) {
        this.orderNo = orderNo;
        this.success = success;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public boolean isSuccess() {
        return success;
    }
}
