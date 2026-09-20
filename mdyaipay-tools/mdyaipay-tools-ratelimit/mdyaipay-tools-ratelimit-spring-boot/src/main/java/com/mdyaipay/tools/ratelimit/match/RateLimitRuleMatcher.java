package com.mdyaipay.tools.ratelimit.match;

import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 按 path（Ant）与 HTTP method 选取第一条命中规则。
 */
public final class RateLimitRuleMatcher {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 返回首条匹配规则。
     * <p>幂等：是。
     *
     * @param rules      规则列表，可为 null
     * @param httpMethod HTTP 方法，可为 null
     * @param path       请求路径
     * @return 命中规则
     */
    public Optional<RateLimitProperties.RuleSpec> firstMatch(
            List<RateLimitProperties.RuleSpec> rules, String httpMethod, String path) {
        if (rules == null || rules.isEmpty() || path == null) {
            return Optional.empty();
        }
        String method = httpMethod == null ? "" : httpMethod.toUpperCase(Locale.ROOT);
        for (RateLimitProperties.RuleSpec rule : rules) {
            if (rule == null || rule.getMatch() == null) {
                continue;
            }
            if (matches(rule.getMatch(), method, path)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /**
     * 单条 match 是否命中。
     */
    private boolean matches(RateLimitProperties.MatchSpec match, String method, String path) {
        String pattern = match.getPath();
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        if (!pathMatcher.match(pattern, path)) {
            return false;
        }
        List<String> methods = match.getMethods();
        if (methods == null || methods.isEmpty()) {
            return true;
        }
        for (String allowed : methods) {
            if (allowed != null && allowed.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }
}
