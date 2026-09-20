package com.mdyaipay.tools.ratelimit.match;

import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RateLimitDubboRuleMatcher} 单测。
 */
class RateLimitDubboRuleMatcherTest {

    private final RateLimitDubboRuleMatcher matcher = new RateLimitDubboRuleMatcher();


    /**
     * provider 侧按接口命中。
     */
    @Test
    void matchesProviderByService() {
        RateLimitProperties.RuleSpec rule = dubboRule("payment-provider", "provider", "com.example.Facade", null);
        var hit = matcher.firstMatch(List.of(rule), "provider", "com.example.Facade", "collect");
        assertTrue(hit.isPresent());
        assertEquals("payment-provider", hit.get().getId());
    }

    /**
     * consumer 与 provider 规则隔离。
     */
    @Test
    void consumerDoesNotMatchProviderRule() {
        RateLimitProperties.RuleSpec rule = dubboRule("p", "provider", "com.example.Facade", null);
        assertTrue(matcher.firstMatch(List.of(rule), "consumer", "com.example.Facade", "collect").isEmpty());
    }

    /**
     * 方法名过滤。
     */
    @Test
    void methodMustMatchWhenConfigured() {
        RateLimitProperties.RuleSpec rule = dubboRule("m", "consumer", "com.example.Facade", "collect");
        assertTrue(matcher.firstMatch(List.of(rule), "consumer", "com.example.Facade", "collect").isPresent());
        assertTrue(matcher.firstMatch(List.of(rule), "consumer", "com.example.Facade", "payout").isEmpty());
    }

    private static RateLimitProperties.RuleSpec dubboRule(
            String id, String side, String service, String method) {
        RateLimitProperties.RuleSpec rule = new RateLimitProperties.RuleSpec();
        rule.setId(id);
        RateLimitProperties.MatchSpec match = new RateLimitProperties.MatchSpec();
        match.setDubboSide(side);
        match.setDubboService(service);
        match.setDubboMethod(method);
        rule.setMatch(match);
        return rule;
    }
}
