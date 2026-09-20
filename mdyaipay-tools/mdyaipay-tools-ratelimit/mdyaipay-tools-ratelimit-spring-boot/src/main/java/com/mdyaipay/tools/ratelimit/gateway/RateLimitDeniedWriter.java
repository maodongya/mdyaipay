package com.mdyaipay.tools.ratelimit.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.model.ApiResponse;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 将限流拒绝写为 429/503 JSON 与标准响应头。
 */
public final class RateLimitDeniedWriter {

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper JSON 序列化，非 null
     */
    public RateLimitDeniedWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 写入 429 限流拒绝。
     */
    public Mono<Void> write429(ServerWebExchange exchange, RateLimitDecision decision) {
        return write(exchange, HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED, decision);
    }

    /**
     * 写入 503 后端不可用。
     */
    public Mono<Void> write503(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.RATE_LIMIT_BACKEND_UNAVAILABLE, null);
    }

    /**
     * 写状态、头与 ApiResponse body。
     */
    private Mono<Void> write(
            ServerWebExchange exchange,
            HttpStatus status,
            ErrorCode errorCode,
            RateLimitDecision decision) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (decision != null) {
            writeRateLimitHeaders(response, decision);
        }
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(ApiResponse.fail(errorCode));
        } catch (Exception ex) {
            body = ("{\"code\":" + errorCode.getCode() + ",\"message\":\"" + errorCode.getMessage() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 写入 Retry-After 与 X-RateLimit-* 头。
     */
    static void writeRateLimitHeaders(ServerHttpResponse response, RateLimitDecision decision) {
        Duration retryAfter = decision.retryAfter();
        long millis = retryAfter == null || retryAfter.isNegative() ? 1000L : Math.max(1L, retryAfter.toMillis());
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        response.getHeaders().set("Retry-After", Long.toString(seconds));
        response.getHeaders().set("X-RateLimit-Limit", Long.toString(decision.limit()));
        response.getHeaders().set("X-RateLimit-Remaining", Long.toString(decision.remaining()));
    }
}
