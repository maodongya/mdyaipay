package com.mdyaipay.gateway.payment;

/** 商户加密收单在网关侧的拒绝原因（映射 HTTP 4xx）。 */
public final class MerchantSignedCollectException extends Exception {

    private final int httpStatus;
    private final String code;

    private MerchantSignedCollectException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public static MerchantSignedCollectException signInvalid() {
        return new MerchantSignedCollectException(401, "SIGN_INVALID", "merchant sign invalid");
    }

    public static MerchantSignedCollectException signExpired() {
        return new MerchantSignedCollectException(401, "SIGN_EXPIRED", "merchant sign expired");
    }

    public static MerchantSignedCollectException merchantMismatch() {
        return new MerchantSignedCollectException(400, "MERCHANT_MISMATCH", "merchant_id mismatch");
    }

    public static MerchantSignedCollectException badPayload() {
        return new MerchantSignedCollectException(400, "BAD_PAYLOAD", "invalid decrypted payload");
    }

    public static MerchantSignedCollectException badRequest(String message) {
        return new MerchantSignedCollectException(400, "BAD_REQUEST", message != null ? message : "bad request");
    }

    public static MerchantSignedCollectException credentialFailed(String message) {
        return new MerchantSignedCollectException(403, "CREDENTIAL_DENIED", message != null ? message : "credential denied");
    }
}
