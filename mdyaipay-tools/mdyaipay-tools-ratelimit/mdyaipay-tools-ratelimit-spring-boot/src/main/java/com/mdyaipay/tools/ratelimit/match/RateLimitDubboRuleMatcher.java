package com.mdyaipay.tools.ratelimit.match;

import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 按 Dubbo 侧（provider/consumer）、接口 FQN、方法名匹配首条规则。
 */
public final class RateLimitDubboRuleMatcher {

    /**
     * 返回首条 Dubbo 规则。
     * <p>幂等：是。
     *
     * @param rules   规则列表
     * @param side    {@code provider} 或 {@code consumer}
     * @param service 接口全限定名
     * @param method  方法名
     * @return 命中规则
     */
    public Optional<RateLimitProperties.RuleSpec> firstMatch(
            List<RateLimitProperties.RuleSpec> rules, String side, String service, String method) {
        if (rules == null || rules.isEmpty() || side == null || service == null) {
            return Optional.empty();
        }
        String sideNorm = side.toLowerCase(Locale.ROOT);
        for (RateLimitProperties.RuleSpec rule : rules) {
            if (rule == null || rule.getMatch() == null) {
                continue;
            }
            if (matches(rule.getMatch(), sideNorm, service, method)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    private boolean matches(
            RateLimitProperties.MatchSpec match, String side, String service, String method) {
        String ruleSide = match.getDubboSide();
        if (ruleSide == null || ruleSide.isBlank()) {
            return false;
        }
        if (!ruleSide.equalsIgnoreCase(side)) {
            return false;
        }
        String ruleService = match.getDubboService();
        if (ruleService == null || ruleService.isBlank()) {
            return false;
        }
        if (!ruleService.equals(service)) {
            return false;
        }
        String ruleMethod = match.getDubboMethod();
        if (ruleMethod == null || ruleMethod.isBlank()) {
            return true;
        }
        return ruleMethod.equals(method);
    }
}
