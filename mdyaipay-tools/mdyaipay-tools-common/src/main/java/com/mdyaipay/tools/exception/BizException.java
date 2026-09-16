package com.mdyaipay.tools.exception;

public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode.getCode(), errorCode.getMessage());
    }

    public static BizException of(ErrorCode errorCode, String detail) {
        return new BizException(errorCode.getCode(), errorCode.getMessage() + ": " + detail);
    }
}
