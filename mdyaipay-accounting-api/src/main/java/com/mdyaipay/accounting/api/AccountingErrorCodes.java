package com.mdyaipay.accounting.api;

/**
 * 账务域业务错误码（与平台 {@link com.mdyaipay.tools.exception.ErrorCode} 并存）。
 */
public final class AccountingErrorCodes {

    /** 钱包不存在。 */
    public static final int WALLET_NOT_FOUND = 10201;
    /** 余额不足（可用或可提现不足）。 */
    public static final int INSUFFICIENT_BALANCE = 10202;
    /** 冻结金额不足。 */
    public static final int INSUFFICIENT_FROZEN = 10203;
    /** 待结算金额不足。 */
    public static final int INSUFFICIENT_PENDING_SETTLE = 10204;
    /** 乐观锁冲突，调用方可重试。 */
    public static final int VERSION_CONFLICT = 10205;
    /** 非法金额或参数。 */
    public static final int INVALID_ARGUMENT = 10206;

    private AccountingErrorCodes() {
    }
}
