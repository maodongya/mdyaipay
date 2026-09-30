package com.mdyaipay.finance.domain.reconcile;

/**
 * 渠道对账差异类型。平账不记差异行。
 */
public enum DifferenceType {
    /** 渠道交易号相同，金额不同。 */
    AMOUNT_MISMATCH,
    /** 我方成功单有，渠道账单无。 */
    LOCAL_ONLY,
    /** 渠道账单有，我方成功单无。 */
    CHANNEL_ONLY
}
