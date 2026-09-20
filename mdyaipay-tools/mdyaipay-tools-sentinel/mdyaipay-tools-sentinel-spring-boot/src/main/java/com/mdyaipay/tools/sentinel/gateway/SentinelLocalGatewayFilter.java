package com.mdyaipay.tools.sentinel.gateway;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitPolicyFactory;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.ratelimit.gateway.RateLimitDeniedWriter;
import com.mdyaipay.tools.ratelimit.match.RateLimitRuleMatcher;
import com.mdyaipay.tools.sentinel.SentinelRuleNames;
import com.mdyaipay.tools.sentinel.autoconfigure.MdyaipaySentinelProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Gateway 本机 Sentinel 限流：资源 {@code local:{ruleId}}，在 Redis 整体限流之前执行。
 */
public final class SentinelLocalGatewayFilter implements GlobalFilter, Ordered {

    private final MdyaipaySentinelProperties sentinelProperties;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimitRuleMatcher ruleMatcher;
    private final RateLimitDeniedWriter deniedWriter;

    /**
     * @param sentinelProperties Sentinel 开关
     * @param rateLimitProperties 规则匹配来源
     * @param ruleMatcher HTTP 规则匹配
     * @param deniedWriter 429 响应
     */
    public SentinelLocalGatewayFilter(
            MdyaipaySentinelProperties sentinelProperties,
            RateLimitProperties rateLimitProperties,
            RateLimitRuleMatcher ruleMatcher,
            RateLimitDeniedWriter deniedWriter) {
        this.sentinelProperties = sentinelProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.ruleMatcher = ruleMatcher;
        this.deniedWriter = deniedWriter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!sentinelProperties.isEnabled() || !rateLimitProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() == null ? null : request.getMethod().name();
        Optional<RateLimitProperties.RuleSpec> matched =
                ruleMatcher.firstMatch(rateLimitProperties.getRules(), method, path);
        if (matched.isEmpty()) {
            return chain.filter(exchange);
        }
        String ruleId = matched.get().getId();
        if (ruleId == null || ruleId.isBlank()) {
            return chain.filter(exchange);
        }
        String resource = SentinelRuleNames.localResource(ruleId);
        return Mono.fromCallable(() -> SphU.entry(resource))
                .flatMap(entry -> chain.filter(exchange).doFinally(signal -> entry.exit()))
                .onErrorResume(BlockException.class, ex -> {
                    RateLimitPolicy policy = RateLimitPolicyFactory.merge(
                            matched.get().getPolicy(), rateLimitProperties.getDefaultPolicy());
                    RateLimitDecision decision = new RateLimitDecision(
                            false, 0L, policy.limit(), Duration.ofSeconds(1), policy.algorithm());
                    return deniedWriter.write429(exchange, decision);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 4;
    }
}
