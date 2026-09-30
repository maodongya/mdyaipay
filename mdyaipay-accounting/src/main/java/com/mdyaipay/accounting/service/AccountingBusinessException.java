package com.mdyaipay.accounting.service;

/** 账务域可预期业务失败，携带 {@link com.mdyaipay.accounting.api.AccountingErrorCodes} 整型码。 */
public final class AccountingBusinessException extends RuntimeException {

    private final int errorCode;

    /**
     * @param errorCode 业务错误码
     * @param message   可读说明
     */
    public AccountingBusinessException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 返回业务错误码。 */
    public int getErrorCode() {
        return errorCode;
    }
}
