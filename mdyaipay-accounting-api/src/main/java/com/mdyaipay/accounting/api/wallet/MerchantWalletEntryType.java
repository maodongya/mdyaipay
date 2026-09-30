package com.mdyaipay.accounting.api.wallet;

/**
 * 商户钱包流水类型。
 */
public enum MerchantWalletEntryType {
    /** 收单成功增加待结算。 */
    PENDING_SETTLE_CREDIT,
    /** 待结算划转到可提现（T+N 结算批次）。 */
    PENDING_TO_WITHDRAWABLE,
    /** 提现扣减可提现余额。 */
    WITHDRAW_DEBIT,
    /** 调账增加可用余额。 */
    AVAILABLE_CREDIT
}
