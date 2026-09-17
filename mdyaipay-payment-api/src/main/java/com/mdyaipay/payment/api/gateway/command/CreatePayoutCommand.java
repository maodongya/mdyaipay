package com.mdyaipay.payment.api.gateway.command;

import java.io.Serializable;

/** 发起代付：{@code payoutNo} 为幂等键，{@code payeeRef} 为收款方标识。 */
public final class CreatePayoutCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String payoutNo;
    private final long amount;
    private final String channel;
    private final String payeeRef;

    public CreatePayoutCommand(String payoutNo, long amount, String channel, String payeeRef) {
        this.payoutNo = payoutNo;
        this.amount = amount;
        this.channel = channel;
        this.payeeRef = payeeRef;
    }

    public String getPayoutNo() {
        return payoutNo;
    }

    public long getAmount() {
        return amount;
    }

    public String getChannel() {
        return channel;
    }

    public String getPayeeRef() {
        return payeeRef;
    }
}
