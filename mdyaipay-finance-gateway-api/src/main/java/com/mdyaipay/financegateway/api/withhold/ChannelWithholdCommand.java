package com.mdyaipay.financegateway.api.withhold;

import java.io.Serializable;

/** 代扣渠道提交指令。 */
public final class ChannelWithholdCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deductionNo;
    private long amount;
    private String channel;
    private String agreementNo;

    public ChannelWithholdCommand() {
    }

    public ChannelWithholdCommand(String deductionNo, long amount, String channel, String agreementNo) {
        this.deductionNo = deductionNo;
        this.amount = amount;
        this.channel = channel;
        this.agreementNo = agreementNo;
    }

    public String getDeductionNo() {
        return deductionNo;
    }

    public void setDeductionNo(String deductionNo) {
        this.deductionNo = deductionNo;
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

    public String getAgreementNo() {
        return agreementNo;
    }

    public void setAgreementNo(String agreementNo) {
        this.agreementNo = agreementNo;
    }
}
