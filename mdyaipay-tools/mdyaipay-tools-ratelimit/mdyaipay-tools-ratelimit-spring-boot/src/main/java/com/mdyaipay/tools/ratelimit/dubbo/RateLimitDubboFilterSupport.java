package com.mdyaipay.tools.ratelimit.dubbo;

import com.mdyaipay.tools.ratelimit.RateLimitContext;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitPolicyFactory;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.ratelimit.match.RateLimitDubboRuleMatcher;
import com.mdyaipay.tools.ratelimit.match.RateLimitKeyComposer;
import com.mdyaipay.tools.ratelimit.observe.RateLimitMetrics;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Dubbo Provider/Consumer 共用的限流判定逻辑。
 */
public final class RateLimitDubboFilterSupport {

    private static final Logger log = LoggerFactory.getLogger(RateLimitDubboFilterSupport.class);
    /** Dubbo 限流拒绝（与 HTTP 429 语义对齐）。 */
    static final int RPC_RATE_LIMITED = 429;
    /** 限流后端不可用且 fail-closed。 */
    static final int RPC_RATE_LIMIT_BACKEND = 503;

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final RateLimitDubboRuleMatcher ruleMatcher;
    private final RateLimitKeyComposer keyComposer;
    private final Map<String, RateLimitKeyResolver> resolvers;
    private final RateLimitMetrics metrics;

    /**
     * @param properties   限流配置
     * @param rateLimiter  后端
     * @param ruleMatcher  Dubbo 规则匹配
     * @param keyComposer  键组合
     * @param resolvers    解析器
     * @param metrics      观测
     */
    public RateLimitDubboFilterSupport(
            RateLimitProperties properties,
            RateLimiter rateLimiter,
            RateLimitDubboRuleMatcher ruleMatcher,
            RateLimitKeyComposer keyComposer,
            Map<String, RateLimitKeyResolver> resolvers,
            RateLimitMetrics metrics) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.ruleMatcher = ruleMatcher;
        this.keyComposer = keyComposer;
        this.resolvers = resolvers;
        this.metrics = metrics == null ? new RateLimitMetrics(null) : metrics;
    }

    /**
     * 若应拒绝或后端 fail-closed 则抛 {@link RpcException}；否则无操作（继续调用链）。
     *
     * @param side       {@code provider} 或 {@code consumer}
     * @param invoker    Dubbo invoker
     * @param invocation 当前调用
     */
    void checkOrThrow(String side, Invoker<?> invoker, Invocation invocation) {
        if (!properties.isEnabled() || !properties.getDubbo().isEnabled()) {
            return;
        }
        if (properties.getRules() == null || properties.getRules().isEmpty()) {
            return;
        }
        String service = invoker.getInterface().getName();
        String method = invocation.getMethodName();
        Optional<RateLimitProperties.RuleSpec> matched =
                ruleMatcher.firstMatch(properties.getRules(), side, service, method);
        if (matched.isEmpty()) {
            return;
        }
        RateLimitProperties.RuleSpec rule = matched.get();
        String ruleId = rule.getId() == null || rule.getId().isBlank() ? "unnamed" : rule.getId();
        String backend = properties.getBackend();
        String pathLabel = service + "#" + method;
        RateLimitContext context = dubboContext(side, service, method);
        Optional<String> key = keyComposer.compose(resolvers, rule.getKeyResolvers(), context);
        if (key.isEmpty()) {
            metrics.record(
                    ruleId, RateLimitMetrics.OUTCOME_SKIPPED, backend, pathLabel, 0L, 0L, null, false);
            return;
        }
        RateLimitPolicy policy = RateLimitPolicyFactory.merge(rule.getPolicy(), properties.getDefaultPolicy());
        long startNanos = System.nanoTime();
        try {
            RateLimitDecision decision = rateLimiter.tryAcquire(key.get(), policy);
            long durationNanos = System.nanoTime() - startNanos;
            if (decision.allowed()) {
                metrics.record(
                        ruleId,
                        RateLimitMetrics.OUTCOME_ALLOWED,
                        backend,
                        pathLabel,
                        decision.limit(),
                        durationNanos,
                        null,
                        false);
                return;
            }
            metrics.record(
                    ruleId,
                    RateLimitMetrics.OUTCOME_DENIED,
                    backend,
                    pathLabel,
                    decision.limit(),
                    durationNanos,
                    null,
                    false);
            throw new RpcException(RPC_RATE_LIMITED, "rate limited: rule=" + ruleId);
        } catch (RpcException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            long durationNanos = System.nanoTime() - startNanos;
            boolean failOpen = properties.isFailOpen();
            metrics.record(
                    ruleId,
                    RateLimitMetrics.OUTCOME_BACKEND_ERROR,
                    backend,
                    pathLabel,
                    policy.limit(),
                    durationNanos,
                    ex.getClass().getSimpleName(),
                    failOpen);
            if (failOpen) {
                log.warn("dubbo rate limit backend failed, fail-open: {}", ex.toString());
                return;
            }
            log.warn("dubbo rate limit backend failed, fail-closed: {}", ex.toString());
            throw new RpcException(RPC_RATE_LIMIT_BACKEND, "rate limit backend unavailable");
        }
    }

    private static RateLimitContext dubboContext(String side, String service, String method) {
        return new RateLimitContext(
                null,
                null,
                null,
                null,
                null,
                Map.of(
                        "dubboSide", side,
                        "dubboService", service,
                        "dubboMethod", method));
    }
}
