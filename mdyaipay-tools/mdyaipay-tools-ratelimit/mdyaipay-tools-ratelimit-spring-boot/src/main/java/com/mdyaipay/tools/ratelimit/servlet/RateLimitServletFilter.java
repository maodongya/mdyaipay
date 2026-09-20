package com.mdyaipay.tools.ratelimit.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.model.ApiResponse;
import com.mdyaipay.tools.ratelimit.RateLimitContext;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitPolicyFactory;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.ratelimit.match.RateLimitKeyComposer;
import com.mdyaipay.tools.ratelimit.match.RateLimitRuleMatcher;
import com.mdyaipay.tools.ratelimit.observe.RateLimitMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Servlet 限流 Filter：规则模型与 Gateway 一致；需显式 {@code servlet.enabled=true}。
 * <p>
 * M1/P2 观测与 Gateway 共用 {@link RateLimitMetrics}。
 */
public final class RateLimitServletFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitServletFilter.class);

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final RateLimitRuleMatcher ruleMatcher;
    private final RateLimitKeyComposer keyComposer;
    private final Map<String, RateLimitKeyResolver> resolvers;
    private final ObjectMapper objectMapper;
    private final RateLimitMetrics metrics;

    /**
     * @param properties   配置
     * @param rateLimiter  限流器
     * @param ruleMatcher  匹配器
     * @param keyComposer  键组合
     * @param resolvers    解析器
     * @param objectMapper JSON
     * @param metrics      观测，可为 null
     */
    public RateLimitServletFilter(
            RateLimitProperties properties,
            RateLimiter rateLimiter,
            RateLimitRuleMatcher ruleMatcher,
            RateLimitKeyComposer keyComposer,
            Map<String, RateLimitKeyResolver> resolvers,
            ObjectMapper objectMapper,
            RateLimitMetrics metrics) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.ruleMatcher = ruleMatcher;
        this.keyComposer = keyComposer;
        this.resolvers = resolvers;
        this.objectMapper = objectMapper;
        this.metrics = metrics == null ? new RateLimitMetrics(null) : metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || properties.getRules() == null || properties.getRules().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        Optional<RateLimitProperties.RuleSpec> matched =
                ruleMatcher.firstMatch(properties.getRules(), method, path);
        if (matched.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        RateLimitProperties.RuleSpec rule = matched.get();
        String ruleId = rule.getId() == null || rule.getId().isBlank() ? "unnamed" : rule.getId();
        String backend = properties.getBackend();
        RateLimitContext context = new RateLimitContext(
                null,
                method,
                path,
                request.getRemoteAddr(),
                firstNonBlank(request.getHeader("X-App-Key"), request.getParameter("appKey")),
                Map.of());
        Optional<String> key = keyComposer.compose(resolvers, rule.getKeyResolvers(), context);
        if (key.isEmpty()) {
            metrics.record(
                    ruleId, RateLimitMetrics.OUTCOME_SKIPPED, backend, path, 0L, 0L, null, false);
            filterChain.doFilter(request, response);
            return;
        }
        /* 功能块：tryAcquire — 与 Gateway 一致的观测与 429/503 */
        RateLimitPolicy policy = RateLimitPolicyFactory.merge(rule.getPolicy(), properties.getDefaultPolicy());
        long startNanos = System.nanoTime();
        try {
            RateLimitDecision decision = rateLimiter.tryAcquire(key.get(), policy);
            long durationNanos = System.nanoTime() - startNanos;
            if (decision.allowed()) {
                stampRequest(request, ruleId, RateLimitMetrics.OUTCOME_ALLOWED);
                metrics.record(
                        ruleId,
                        RateLimitMetrics.OUTCOME_ALLOWED,
                        backend,
                        path,
                        decision.limit(),
                        durationNanos,
                        null,
                        false);
                filterChain.doFilter(request, response);
                return;
            }
            stampRequest(request, ruleId, RateLimitMetrics.OUTCOME_DENIED);
            metrics.record(
                    ruleId,
                    RateLimitMetrics.OUTCOME_DENIED,
                    backend,
                    path,
                    decision.limit(),
                    durationNanos,
                    null,
                    false);
            writeDenied(response, 429, ErrorCode.RATE_LIMITED, decision);
        } catch (RuntimeException ex) {
            long durationNanos = System.nanoTime() - startNanos;
            boolean failOpen = properties.isFailOpen();
            stampRequest(request, ruleId, RateLimitMetrics.OUTCOME_BACKEND_ERROR);
            metrics.record(
                    ruleId,
                    RateLimitMetrics.OUTCOME_BACKEND_ERROR,
                    backend,
                    path,
                    policy.limit(),
                    durationNanos,
                    ex.getClass().getSimpleName(),
                    failOpen);
            if (failOpen) {
                log.warn("rate limit backend failed, fail-open: {}", ex.toString());
                filterChain.doFilter(request, response);
                return;
            }
            writeDenied(response, 503, ErrorCode.RATE_LIMIT_BACKEND_UNAVAILABLE, null);
        }
    }

    /**
     * 写入请求属性，供 Trace 与排障关联限流结果。
     */
    private static void stampRequest(HttpServletRequest request, String ruleId, String outcome) {
        request.setAttribute(RateLimitProperties.ATTR_RATE_LIMIT_RULE_ID, ruleId);
        request.setAttribute(RateLimitProperties.ATTR_RATE_LIMIT_OUTCOME, outcome);
    }

    /**
     * 写拒绝响应。
     */
    private void writeDenied(
            HttpServletResponse response, int status, ErrorCode errorCode, RateLimitDecision decision)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (decision != null) {
            Duration retryAfter = decision.retryAfter();
            long millis = retryAfter == null || retryAfter.isNegative() ? 1000L : Math.max(1L, retryAfter.toMillis());
            response.setHeader("Retry-After", Long.toString(Math.max(1L, (millis + 999L) / 1000L)));
            response.setHeader("X-RateLimit-Limit", Long.toString(decision.limit()));
            response.setHeader("X-RateLimit-Remaining", Long.toString(decision.remaining()));
        }
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(errorCode));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
