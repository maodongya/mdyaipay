package com.mdyaipay.tools.ratelimit.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动态更新 {@link RateLimitProperties} 中某条规则的集群额度（limit）。
 * <p>
 * 供 Sentinel Dashboard 推送 {@code cluster:{ruleId}} 流控规则时调用；不负责单机 Sentinel 规则。
 */
public final class RateLimitClusterPolicyRefresher {

    private static final Logger log = LoggerFactory.getLogger(RateLimitClusterPolicyRefresher.class);

    private final RateLimitProperties properties;

    /**
     * @param properties 限流配置 Bean
     */
    public RateLimitClusterPolicyRefresher(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * 按 ruleId 更新 policy.limit；无匹配 id 时打 WARN 并忽略。
     * <p>
     * 幂等：多次写入相同 limit 无副作用。
     *
     * @param ruleId 与 yaml {@code mdyaipay.ratelimit.rules[].id} 一致
     * @param limit  新上限（次/窗口，与既有 policy 窗口一致）
     */
    public void updateLimit(String ruleId, long limit) {
        if (ruleId == null || ruleId.isBlank()) {
            return;
        }
        if (limit <= 0L) {
            log.warn("event=cluster_rule_skip rule_id={} reason=non_positive_limit", ruleId);
            return;
        }
        if (properties.getRules() == null) {
            return;
        }
        for (RateLimitProperties.RuleSpec rule : properties.getRules()) {
            if (rule == null || rule.getId() == null) {
                continue;
            }
            if (!ruleId.equals(rule.getId())) {
                continue;
            }
            RateLimitProperties.PolicySpec policy = rule.getPolicy();
            if (policy == null) {
                policy = new RateLimitProperties.PolicySpec();
                rule.setPolicy(policy);
            }
            Long previous = policy.getLimit();
            policy.setLimit(limit);
            log.info(
                    "event=cluster_rule_updated rule_id={} limit={} previous={}",
                    ruleId,
                    limit,
                    previous);
            return;
        }
        log.warn("event=cluster_rule_unknown rule_id={} limit={}", ruleId, limit);
    }
}
