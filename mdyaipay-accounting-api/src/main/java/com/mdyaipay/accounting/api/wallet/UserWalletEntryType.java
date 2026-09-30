package com.mdyaipay.accounting.api.wallet;

/**
 * 用户钱包流水类型（与 {@link UserWalletEntryCommand#getEntryType()} 对应）。
 */
public enum UserWalletEntryType {
    CREDIT,
    DEBIT,
    FREEZE,
    UNFREEZE
}
