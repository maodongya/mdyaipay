package com.mdyaipay.accounting.api.wallet;

import java.io.Serializable;

/**
 * 用户钱包余额快照。
 */
public final class UserWalletView implements Serializable {

    private static final long serialVersionUID = 1L;

    private long walletId;
    private long userId;
    private long balance;
    private long frozenAmount;
    private long version;

    public UserWalletView() {
    }

    public UserWalletView(long walletId, long userId, long balance, long frozenAmount, long version) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.frozenAmount = frozenAmount;
        this.version = version;
    }

    public long getWalletId() {
        return walletId;
    }

    public void setWalletId(long walletId) {
        this.walletId = walletId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public long getFrozenAmount() {
        return frozenAmount;
    }

    public void setFrozenAmount(long frozenAmount) {
        this.frozenAmount = frozenAmount;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
