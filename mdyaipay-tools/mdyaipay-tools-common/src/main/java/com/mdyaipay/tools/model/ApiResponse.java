package com.mdyaipay.tools.model;

import com.mdyaipay.tools.exception.ErrorCode;

import java.time.Instant;
import java.util.UUID;

/**
 * 统一 API 响应封装。
 *
 * @param <T> 业务数据类型
 */
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = ErrorCode.SUCCESS.getCode();
        response.message = "success";
        response.data = data;
        response.traceId = newTraceId();
        response.timestamp = Instant.now().toString();
        return response;
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = code;
        response.message = message;
        response.traceId = newTraceId();
        response.timestamp = Instant.now().toString();
        return response;
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getTimestamp() {
        return timestamp;
    }

}
