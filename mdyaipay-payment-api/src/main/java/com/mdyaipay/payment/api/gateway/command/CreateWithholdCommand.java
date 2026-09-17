package com.mdyaipay.payment.api.gateway.command;

import java.io.Serializable;

/** 发起代扣：{@code deductionNo} 为幂等键，{@code agreementNo} 为签约协议号。 */
public final class CreateWithholdCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String deductionNo;
    private final String agreementNo;
    private final long amount;
    private final String channel;

    public CreateWithholdCommand(String deductionNo, String agreementNo, long amount, String channel) {
        this.deductionNo = deductionNo;
        this.agreementNo = agreementNo;
        this.amount = amount;
        this.channel = channel;
    }

    public String getDeductionNo() {
        return deductionNo;
    }

    public String getAgreementNo() {
        return agreementNo;
    }

    public long getAmount() {
        return amount;
    }

    public String getChannel() {
        return channel;
    }
}
