package com.mdyaipay.accounting.api.wallet;

import java.io.Serializable;

/**
 * 商户钱包记账指令（幂等键 {@link #bizKey}）。
 */
public final class MerchantWalletEntryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private long merchantId;
    private String bizKey;
    /** 见 {@link MerchantWalletEntryType} 名称。 */
    private String entryType;
    private long amount;

    public MerchantWalletEntryCommand() {
    }

    public MerchantWalletEntryCommand(long merchantId, String bizKey, String entryType, long amount) {
        this.merchantId = merchantId;
        this.bizKey = bizKey;
        this.entryType = entryType;
        this.amount = amount;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(long merchantId) {
        this.merchantId = merchantId;
    }

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
