package com.mdyaipay.tools.sentinel.autoconfigure;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitPolicyFactory;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.sentinel.SentinelRuleNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动时用 {@link RateLimitProperties} 种子化 {@code local:} 本机流控规则（不覆盖 Dashboard 已下发的同名资源）。
 */
public final class SentinelLocalRuleSeeder implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SentinelLocalRuleSeeder.class);

    private final MdyaipaySentinelProperties sentinelProperties;
    private final RateLimitProperties rateLimitProperties;

    /**
     * @param sentinelProperties Sentinel 配置
     * @param rateLimitProperties 整体限流规则（仅读 limit 作默认本机 QPS）
     */
    public SentinelLocalRuleSeeder(
            MdyaipaySentinelProperties sentinelProperties, RateLimitProperties rateLimitProperties) {
        this.sentinelProperties = sentinelProperties;
        this.rateLimitProperties = rateLimitProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!sentinelProperties.isEnabled() || !sentinelProperties.isSeedLocalRulesFromRatelimit()) {
            return;
        }
        if (rateLimitProperties.getRules() == null || rateLimitProperties.getRules().isEmpty()) {
            return;
        }
        Map<String, FlowRule> merged = new LinkedHashMap<>();
        for (FlowRule existing : FlowRuleManager.getRules()) {
            if (existing != null && existing.getResource() != null) {
                merged.put(existing.getResource(), existing);
            }
        }
        for (RateLimitProperties.RuleSpec rule : rateLimitProperties.getRules()) {
            if (rule == null || rule.getId() == null || rule.getId().isBlank()) {
                continue;
            }
            String resource = SentinelRuleNames.localResource(rule.getId());
            if (merged.containsKey(resource)) {
                continue;
            }
            long qps = resolveLocalQps(rule);
            FlowRule flowRule = new FlowRule();
            flowRule.setResource(resource);
            flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            flowRule.setCount(qps);
            merged.put(resource, flowRule);
        }
        FlowRuleManager.loadRules(new ArrayList<>(merged.values()));
        log.info("event=sentinel_local_rules_seeded count={}", merged.size());
    }

    private long resolveLocalQps(RateLimitProperties.RuleSpec rule) {
        if (sentinelProperties.getDefaultLocalQps() != null && sentinelProperties.getDefaultLocalQps() > 0L) {
            return sentinelProperties.getDefaultLocalQps();
        }
        var policy = RateLimitPolicyFactory.merge(rule.getPolicy(), rateLimitProperties.getDefaultPolicy());
        return Math.max(1L, policy.limit());
    }
}
