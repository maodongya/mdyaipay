package com.mdyaipay.financegateway.api.payout;

import java.io.Serializable;

/** 代付渠道提交指令。 */
public final class ChannelPayoutCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private String payoutNo;
    private long amount;
    private String channel;

    public ChannelPayoutCommand() {
    }

    public ChannelPayoutCommand(String payoutNo, long amount, String channel) {
        this.payoutNo = payoutNo;
        this.amount = amount;
        this.channel = channel;
    }

    public String getPayoutNo() {
        return payoutNo;
    }

    public void setPayoutNo(String payoutNo) {
        this.payoutNo = payoutNo;
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
}
