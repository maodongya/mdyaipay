package com.mdyaipay.finance.domain.reconcile;

/**
 * 账单进入批次的方式。
 */
public enum BillSource {
    /** 从 CSV 文件读入。 */
    FILE,
    /** 向渠道拉取。 */
    CHANNEL_PULL
}
