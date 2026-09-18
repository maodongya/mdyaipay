package com.mdyaipay.tools.model;

import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceIds;

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
        response.traceId = resolveTraceId();
        response.timestamp = Instant.now().toString();
        return response;
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = code;
        response.message = message;
        response.traceId = resolveTraceId();
        response.timestamp = Instant.now().toString();
        return response;
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 优先使用当前 {@link TraceContext} 的 traceId（展示为 12 位）；无上下文时生成随机 id。
     */
    private static String resolveTraceId() {
        return TraceContext.currentTraceId()
                .map(TraceIds::toDisplayTraceId)
                .orElseGet(ApiResponse::fallbackTraceId);
    }

    /** 无 Trace 上下文时的回退 id（12 位 hex）。 */
    private static String fallbackTraceId() {
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
