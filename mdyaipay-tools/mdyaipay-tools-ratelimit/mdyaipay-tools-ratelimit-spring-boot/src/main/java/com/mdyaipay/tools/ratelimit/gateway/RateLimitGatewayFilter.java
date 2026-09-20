package com.mdyaipay.tools.ratelimit.gateway;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;

/**
 * Gateway 全局限流：匹配首条规则后 {@link RateLimiter#tryAcquire}，拒绝则 429。
 * <p>
 * Order = {@link Ordered#HIGHEST_PRECEDENCE} + 5，位于 Trace 之后、collect 验签 Filter 之前。
 */
public final class RateLimitGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitGatewayFilter.class);

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final RateLimitRuleMatcher ruleMatcher;
    private final RateLimitKeyComposer keyComposer;
    private final Map<String, RateLimitKeyResolver> resolvers;
    private final RateLimitDeniedWriter deniedWriter;
    private final RateLimitMetrics metrics;

    /**
     * @param properties   配置
     * @param rateLimiter  后端限流器
     * @param ruleMatcher  规则匹配
     * @param keyComposer  键组合
     * @param resolvers    内置解析器
     * @param deniedWriter 拒绝写入
     * @param metrics      M1 观测，可为 null
     */
    public RateLimitGatewayFilter(
            RateLimitProperties properties,
            RateLimiter rateLimiter,
            RateLimitRuleMatcher ruleMatcher,
            RateLimitKeyComposer keyComposer,
            Map<String, RateLimitKeyResolver> resolvers,
            RateLimitDeniedWriter deniedWriter,
            RateLimitMetrics metrics) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.ruleMatcher = ruleMatcher;
        this.keyComposer = keyComposer;
        this.resolvers = resolvers;
        this.deniedWriter = deniedWriter;
        this.metrics = metrics == null ? new RateLimitMetrics(null) : metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled() || properties.getRules() == null || properties.getRules().isEmpty()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() == null ? null : request.getMethod().name();
        Optional<RateLimitProperties.RuleSpec> matched =
                ruleMatcher.firstMatch(properties.getRules(), method, path);
        if (matched.isEmpty()) {
            return chain.filter(exchange);
        }
        RateLimitProperties.RuleSpec rule = matched.get();
        String ruleId = rule.getId() == null || rule.getId().isBlank() ? "unnamed" : rule.getId();
        String backend = properties.getBackend();
        RateLimitContext context = buildContext(exchange, path, method);
        Optional<String> key = keyComposer.compose(resolvers, rule.getKeyResolvers(), context);
        if (key.isEmpty()) {
            metrics.record(
                    ruleId, RateLimitMetrics.OUTCOME_SKIPPED, backend, path, 0L, 0L, null, false);
            return chain.filter(exchange);
        }
        /* 功能块：tryAcquire — 观测、429/503 或放行 */
        RateLimitPolicy policy = RateLimitPolicyFactory.merge(rule.getPolicy(), properties.getDefaultPolicy());
        long startNanos = System.nanoTime();
        try {
            RateLimitDecision decision = rateLimiter.tryAcquire(key.get(), policy);
            long durationNanos = System.nanoTime() - startNanos;
            if (decision.allowed()) {
                stampExchange(exchange, ruleId, RateLimitMetrics.OUTCOME_ALLOWED);
                metrics.record(
                        ruleId,
                        RateLimitMetrics.OUTCOME_ALLOWED,
                        backend,
                        path,
                        decision.limit(),
                        durationNanos,
                        null,
                        false);
                return chain.filter(exchange);
            }
            stampExchange(exchange, ruleId, RateLimitMetrics.OUTCOME_DENIED);
            metrics.record(
                    ruleId,
                    RateLimitMetrics.OUTCOME_DENIED,
                    backend,
                    path,
                    decision.limit(),
                    durationNanos,
                    null,
                    false);
            return deniedWriter.write429(exchange, decision);
        } catch (RuntimeException ex) {
            long durationNanos = System.nanoTime() - startNanos;
            boolean failOpen = properties.isFailOpen();
            stampExchange(exchange, ruleId, RateLimitMetrics.OUTCOME_BACKEND_ERROR);
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
                return chain.filter(exchange);
            }
            log.warn("rate limit backend failed, fail-closed: {}", ex.toString());
            return deniedWriter.write503(exchange);
        }
    }

    /**
     * 写入 Exchange 属性，供 Trace 与排障关联限流结果。
     */
    private static void stampExchange(ServerWebExchange exchange, String ruleId, String outcome) {
        exchange.getAttributes().put(RateLimitProperties.ATTR_RATE_LIMIT_RULE_ID, ruleId);
        exchange.getAttributes().put(RateLimitProperties.ATTR_RATE_LIMIT_OUTCOME, outcome);
    }

    /**
     * 从 Exchange 填充上下文（不读 body）。
     */
    private static RateLimitContext buildContext(ServerWebExchange exchange, String path, String method) {
        String clientIp = resolveClientIp(exchange.getRequest());
        String routeId = null;
        Object routeAttr = exchange.getAttributes().get(
                "org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRouteIdAttr");
        if (routeAttr instanceof String s && !s.isBlank()) {
            routeId = s;
        }
        Object attrAppKey = exchange.getAttribute(RateLimitProperties.ATTR_MERCHANT_APP_KEY);
        String merchantAppKey = attrAppKey instanceof String s && !s.isBlank()
                ? s
                : firstNonBlank(
                        exchange.getRequest().getHeaders().getFirst("X-App-Key"),
                        exchange.getRequest().getQueryParams().getFirst("appKey"));
        return new RateLimitContext(routeId, method, path, clientIp, merchantAppKey, Map.of());
    }

    /**
     * 解析客户端 IP。
     */
    private static String resolveClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return null;
        }
        return remote.getAddress().getHostAddress();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
