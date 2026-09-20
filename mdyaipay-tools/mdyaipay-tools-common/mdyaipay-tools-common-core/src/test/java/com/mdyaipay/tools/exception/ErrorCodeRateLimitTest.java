package com.mdyaipay.tools.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 限流相关 {@link ErrorCode} 单测。
 */
class ErrorCodeRateLimitTest {

    /**
     * 验证限流与后端不可用业务码已定义。
     */
    @Test
    void rateLimitCodesDefined() {
        assertEquals(42900, ErrorCode.RATE_LIMITED.getCode());
        assertEquals(50301, ErrorCode.RATE_LIMIT_BACKEND_UNAVAILABLE.getCode());
    }
}
