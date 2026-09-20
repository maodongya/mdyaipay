package com.mdyaipay.tools.ratelimit.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.ratelimit.match.RateLimitKeyComposer;
import com.mdyaipay.tools.ratelimit.match.RateLimitRuleMatcher;
import com.mdyaipay.tools.ratelimit.observe.RateLimitMetrics;
import com.mdyaipay.tools.ratelimit.resolve.PathKeyResolver;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RateLimitGatewayFilter} 拒绝路径单测。
 */
class RateLimitGatewayFilterTest {

    /**
     * Mock 限流拒绝时返回 429 且不继续 chain。
     */
    @Test
    void rejectsWith429WhenLimiterDenies() {
        RateLimitProperties properties = new RateLimitProperties();
        RateLimitProperties.RuleSpec rule = new RateLimitProperties.RuleSpec();
        rule.setId("r1");
        RateLimitProperties.MatchSpec match = new RateLimitProperties.MatchSpec();
        match.setPath("/api/v1/payments/collect");
        match.setMethods(List.of("POST"));
        rule.setMatch(match);
        RateLimitProperties.PolicySpec policy = new RateLimitProperties.PolicySpec();
        policy.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        policy.setLimit(1L);
        policy.setWindow(Duration.ofSeconds(1));
        policy.setRefillRatePerSecond(1.0d);
        rule.setPolicy(policy);
        rule.setKeyResolvers(List.of("path"));
        properties.setRules(List.of(rule));

        RateLimiter limiter = (key, p) -> new RateLimitDecision(
                false, 0, 1, Duration.ofMillis(500), RateLimitAlgorithm.TOKEN_BUCKET);
        Map<String, RateLimitKeyResolver> resolvers = Map.of("path", new PathKeyResolver());
        RateLimitGatewayFilter filter = new RateLimitGatewayFilter(
                properties,
                limiter,
                new RateLimitRuleMatcher(),
                new RateLimitKeyComposer(),
                resolvers,
                new RateLimitDeniedWriter(new ObjectMapper()),
                new RateLimitMetrics(null));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/payments/collect").build());
        AtomicBoolean continued = new AtomicBoolean();
        GatewayFilterChain chain = ex -> {
            continued.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertFalse(continued.get());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getResponse().getHeaders().containsKey("Retry-After"));
        assertEquals("1", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    /**
     * 验证业务码常量与 Filter Order。
     */
    @Test
    void orderAndErrorCode() {
        RateLimitGatewayFilter filter = new RateLimitGatewayFilter(
                new RateLimitProperties(),
                (k, p) -> new RateLimitDecision(true, 1, 1, Duration.ZERO, RateLimitAlgorithm.FIXED_WINDOW),
                new RateLimitRuleMatcher(),
                new RateLimitKeyComposer(),
                Map.of(),
                new RateLimitDeniedWriter(new ObjectMapper()),
                new RateLimitMetrics(null));
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 5, filter.getOrder());
        assertEquals(42900, ErrorCode.RATE_LIMITED.getCode());
    }
}
