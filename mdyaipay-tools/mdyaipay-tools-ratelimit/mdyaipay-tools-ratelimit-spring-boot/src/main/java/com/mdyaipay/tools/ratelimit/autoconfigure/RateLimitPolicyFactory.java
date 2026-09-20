package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;

import java.time.Duration;

/**
 * 将 rule.policy 与 default-policy 合并为不可变 {@link RateLimitPolicy}。
 */
public final class RateLimitPolicyFactory {

    private RateLimitPolicyFactory() {
    }

    /**
     * 合并策略；rule 字段优先，缺省回落 defaultPolicy。
     *
     * @param rulePolicy    规则策略，可为 null
     * @param defaultPolicy 默认策略，可为 null
     * @return 完整策略
     */
    public static RateLimitPolicy merge(
            RateLimitProperties.PolicySpec rulePolicy,
            RateLimitProperties.PolicySpec defaultPolicy) {
        RateLimitAlgorithm algorithm = firstNonNull(
                rulePolicy == null ? null : rulePolicy.getAlgorithm(),
                defaultPolicy == null ? null : defaultPolicy.getAlgorithm());
        Long limit = firstNonNull(
                rulePolicy == null ? null : rulePolicy.getLimit(),
                defaultPolicy == null ? null : defaultPolicy.getLimit());
        Duration window = firstNonNull(
                rulePolicy == null ? null : rulePolicy.getWindow(),
                defaultPolicy == null ? null : defaultPolicy.getWindow());
        Double refill = firstNonNull(
                rulePolicy == null ? null : rulePolicy.getRefillRatePerSecond(),
                defaultPolicy == null ? null : defaultPolicy.getRefillRatePerSecond());
        Integer segments = firstNonNull(
                rulePolicy == null ? null : rulePolicy.getSlidingSegments(),
                defaultPolicy == null ? null : defaultPolicy.getSlidingSegments());

        if (algorithm == null) {
            throw new IllegalStateException("rate limit algorithm is required");
        }
        if (limit == null) {
            throw new IllegalStateException("rate limit limit is required");
        }
        if (window == null) {
            throw new IllegalStateException("rate limit window is required");
        }
        double refillRate = refill == null ? 0.0d : refill;
        int slidingSegments = segments == null ? 0 : segments;
        return new RateLimitPolicy(algorithm, limit, window, refillRate, slidingSegments);
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
