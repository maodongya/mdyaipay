package com.mdyaipay.accounting.domain.wallet;

import com.mdyaipay.accounting.api.AccountingErrorCodes;
import com.mdyaipay.accounting.api.wallet.MerchantWalletEntryType;
import com.mdyaipay.accounting.service.AccountingBusinessException;

import java.time.Instant;

/**
 * 商户资金聚合：可用、待结算、可提现三桶余额。
 */
public final class MerchantWallet {

    private final long walletId;
    private final long merchantId;
    private long availableAmount;
    private long pendingSettleAmount;
    private long withdrawableAmount;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private MerchantWallet(
            long walletId,
            long merchantId,
            long availableAmount,
            long pendingSettleAmount,
            long withdrawableAmount,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.walletId = walletId;
        this.merchantId = merchantId;
        this.availableAmount = availableAmount;
        this.pendingSettleAmount = pendingSettleAmount;
        this.withdrawableAmount = withdrawableAmount;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 新建空商户钱包。 */
    public static MerchantWallet open(long walletId, long merchantId, Instant now) {
        return new MerchantWallet(walletId, merchantId, 0L, 0L, 0L, 0L, now, now);
    }

    /** 自持久化层还原。 */
    public static MerchantWallet rehydrate(
            long walletId,
            long merchantId,
            long availableAmount,
            long pendingSettleAmount,
            long withdrawableAmount,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        return new MerchantWallet(
                walletId,
                merchantId,
                availableAmount,
                pendingSettleAmount,
                withdrawableAmount,
                version,
                createdAt,
                updatedAt);
    }

    /** 按分录类型变更三桶余额。 */
    public void applyEntry(MerchantWalletEntryType entryType, long amount) {
        if (amount <= 0) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "amount must be positive");
        }
        switch (entryType) {
            case PENDING_SETTLE_CREDIT -> pendingSettleAmount += amount;
            case PENDING_TO_WITHDRAWABLE -> {
                if (pendingSettleAmount < amount) {
                    throw new AccountingBusinessException(
                            AccountingErrorCodes.INSUFFICIENT_PENDING_SETTLE, "insufficient pending settle");
                }
                pendingSettleAmount -= amount;
                withdrawableAmount += amount;
            }
            case WITHDRAW_DEBIT -> {
                if (withdrawableAmount < amount) {
                    throw new AccountingBusinessException(AccountingErrorCodes.INSUFFICIENT_BALANCE, "insufficient withdrawable");
                }
                withdrawableAmount -= amount;
            }
            case AVAILABLE_CREDIT -> availableAmount += amount;
        }
        version += 1;
        updatedAt = Instant.now();
    }

    public long getWalletId() {
        return walletId;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public long getAvailableAmount() {
        return availableAmount;
    }

    public long getPendingSettleAmount() {
        return pendingSettleAmount;
    }

    public long getWithdrawableAmount() {
        return withdrawableAmount;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
