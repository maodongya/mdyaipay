package com.mdyaipay.user.integration.merchant.sign;

/** 验签结果：成功时带 {@code merchantId}，失败时带 {@link com.mdyaipay.user.api.UserErrorCodes}。 */
public final class SignVerifyResult {

    private final boolean success;
    private final long merchantId;
    private final int errorCode;

    private SignVerifyResult(boolean success, long merchantId, int errorCode) {
        this.success = success;
        this.merchantId = merchantId;
        this.errorCode = errorCode;
    }

    public static SignVerifyResult ok(long merchantId) {
        return new SignVerifyResult(true, merchantId, 0);
    }

    public static SignVerifyResult fail(int errorCode) {
        return new SignVerifyResult(false, 0L, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
