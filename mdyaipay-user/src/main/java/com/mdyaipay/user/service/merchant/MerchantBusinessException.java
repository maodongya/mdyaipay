package com.mdyaipay.user.service.merchant;

/** 商户域可预期业务失败，携带 {@link com.mdyaipay.user.api.UserErrorCodes} 整型码。 */
public final class MerchantBusinessException extends RuntimeException {

    private final int errorCode;

    public MerchantBusinessException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
