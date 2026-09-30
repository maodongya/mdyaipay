package com.mdyaipay.accounting.api.wallet;

import java.io.Serializable;

/**
 * 用户钱包记账指令（幂等键 {@link #bizKey}）。
 */
public final class UserWalletEntryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户主体 ID。 */
    private long userId;
    /** 全局幂等业务键。 */
    private String bizKey;
    /** 见 {@link UserWalletEntryType} 名称。 */
    private String entryType;
    /** 变动金额（分，正数）。 */
    private long amount;

    public UserWalletEntryCommand() {
    }

    public UserWalletEntryCommand(long userId, String bizKey, String entryType, long amount) {
        this.userId = userId;
        this.bizKey = bizKey;
        this.entryType = entryType;
        this.amount = amount;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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
