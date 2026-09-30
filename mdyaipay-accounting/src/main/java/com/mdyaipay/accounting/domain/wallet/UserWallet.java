package com.mdyaipay.accounting.domain.wallet;

import com.mdyaipay.accounting.api.AccountingErrorCodes;
import com.mdyaipay.accounting.api.wallet.UserWalletEntryType;
import com.mdyaipay.accounting.service.AccountingBusinessException;

import java.time.Instant;

/**
 * 用户钱包聚合：可用余额与冻结金额，变更通过 {@link #applyEntry(UserWalletEntryType, long)}。
 */
public final class UserWallet {

    private final long walletId;
    private final long userId;
    private long balance;
    private long frozenAmount;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserWallet(
            long walletId,
            long userId,
            long balance,
            long frozenAmount,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.frozenAmount = frozenAmount;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 新建空钱包（余额与冻结均为 0）。
     */
    public static UserWallet open(long walletId, long userId, Instant now) {
        return new UserWallet(walletId, userId, 0L, 0L, 0L, now, now);
    }

    /**
     * 自持久化层还原。
     */
    public static UserWallet rehydrate(
            long walletId,
            long userId,
            long balance,
            long frozenAmount,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        return new UserWallet(walletId, userId, balance, frozenAmount, version, createdAt, updatedAt);
    }

    /** 按分录类型变更余额/冻结（金额为正数）。 */
    public void applyEntry(UserWalletEntryType entryType, long amount) {
        if (amount <= 0) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "amount must be positive");
        }
        switch (entryType) {
            case CREDIT -> balance += amount;
            case DEBIT -> {
                if (balance < amount) {
                    throw new AccountingBusinessException(AccountingErrorCodes.INSUFFICIENT_BALANCE, "insufficient balance");
                }
                balance -= amount;
            }
            case FREEZE -> {
                if (balance < amount) {
                    throw new AccountingBusinessException(AccountingErrorCodes.INSUFFICIENT_BALANCE, "insufficient balance to freeze");
                }
                balance -= amount;
                frozenAmount += amount;
            }
            case UNFREEZE -> {
                if (frozenAmount < amount) {
                    throw new AccountingBusinessException(AccountingErrorCodes.INSUFFICIENT_FROZEN, "insufficient frozen");
                }
                frozenAmount -= amount;
                balance += amount;
            }
        }
        version += 1;
        updatedAt = Instant.now();
    }

    public long getWalletId() {
        return walletId;
    }

    public long getUserId() {
        return userId;
    }

    public long getBalance() {
        return balance;
    }

    public long getFrozenAmount() {
        return frozenAmount;
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
