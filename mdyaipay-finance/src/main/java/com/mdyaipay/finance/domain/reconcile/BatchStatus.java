package com.mdyaipay.finance.domain.reconcile;

/**
 * 对账批次状态。{@link #PROCESSED} 本期只预留，没有处理接口。
 */
public enum BatchStatus {
    /** 已比对，差异尚未被处理。 */
    OPEN,
    /** 差异已处理，禁止重跑覆盖。 */
    PROCESSED
}
