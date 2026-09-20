package com.mdyaipay.tools.ratelimit.match;

import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RateLimitRuleMatcher} 路径/方法匹配单测。
 */
class RateLimitRuleMatcherTest {

    /**
     * 匹配首条 path+method 规则。
     */
    @Test
    void matchesFirstRuleByPathAndMethod() {
        RateLimitProperties.RuleSpec collect = rule("collect", "/api/v1/payments/collect", List.of("POST"));
        RateLimitProperties.RuleSpec api = rule("api", "/api/v1/**", List.of());
        RateLimitRuleMatcher matcher = new RateLimitRuleMatcher();

        assertEquals("collect", matcher.firstMatch(List.of(collect, api), "POST", "/api/v1/payments/collect").orElseThrow().getId());
        assertEquals("api", matcher.firstMatch(List.of(collect, api), "GET", "/api/v1/other").orElseThrow().getId());
        assertTrue(matcher.firstMatch(List.of(collect), "GET", "/api/v1/payments/collect").isEmpty());
    }

    private static RateLimitProperties.RuleSpec rule(String id, String path, List<String> methods) {
        RateLimitProperties.RuleSpec rule = new RateLimitProperties.RuleSpec();
        rule.setId(id);
        RateLimitProperties.MatchSpec match = new RateLimitProperties.MatchSpec();
        match.setPath(path);
        match.setMethods(methods);
        rule.setMatch(match);
        return rule;
    }
}
