package com.mdyaipay.accounting.api.wallet;

import java.io.Serializable;

/**
 * 商户资金账户快照：可用、待结算、可提现三桶。
 */
public final class MerchantWalletView implements Serializable {

    private static final long serialVersionUID = 1L;

    private long walletId;
    private long merchantId;
    private long availableAmount;
    private long pendingSettleAmount;
    private long withdrawableAmount;
    private long version;

    public MerchantWalletView() {
    }

    public MerchantWalletView(
            long walletId,
            long merchantId,
            long availableAmount,
            long pendingSettleAmount,
            long withdrawableAmount,
            long version) {
        this.walletId = walletId;
        this.merchantId = merchantId;
        this.availableAmount = availableAmount;
        this.pendingSettleAmount = pendingSettleAmount;
        this.withdrawableAmount = withdrawableAmount;
        this.version = version;
    }

    public long getWalletId() {
        return walletId;
    }

    public void setWalletId(long walletId) {
        this.walletId = walletId;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(long merchantId) {
        this.merchantId = merchantId;
    }

    public long getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(long availableAmount) {
        this.availableAmount = availableAmount;
    }

    public long getPendingSettleAmount() {
        return pendingSettleAmount;
    }

    public void setPendingSettleAmount(long pendingSettleAmount) {
        this.pendingSettleAmount = pendingSettleAmount;
    }

    public long getWithdrawableAmount() {
        return withdrawableAmount;
    }

    public void setWithdrawableAmount(long withdrawableAmount) {
        this.withdrawableAmount = withdrawableAmount;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
