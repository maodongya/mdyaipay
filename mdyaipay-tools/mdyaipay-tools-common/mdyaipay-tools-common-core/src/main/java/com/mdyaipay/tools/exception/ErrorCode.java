package com.mdyaipay.tools.exception;

/**
 * 平台统一业务错误码（HTTP 网关与后续 RPC 可共用）。
 */
public enum ErrorCode {
    SUCCESS(0, "success"),
    INVALID_PARAM(10002, "invalid parameter"),
    DUPLICATE_REQUEST(10003, "duplicate request"),
    NOT_FOUND(10004, "resource not found"),
    /** 超过限流配额（HTTP 429）。 */
    RATE_LIMITED(42900, "rate limited"),
    /** 限流后端不可用（HTTP 503，fail-closed）。 */
    RATE_LIMIT_BACKEND_UNAVAILABLE(50301, "rate limit backend unavailable"),
    INTERNAL_ERROR(50000, "internal error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
