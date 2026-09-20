package com.mdyaipay.tools.ratelimit.match;

import com.mdyaipay.tools.ratelimit.RateLimitContext;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 按配置的 resolver 名称列表组合限流键（冒号连接，跳过 null）。
 */
public final class RateLimitKeyComposer {

    /**
     * 组合逻辑键；无有效片段时返回 empty。
     * <p>幂等：是。
     *
     * @param resolversByName 名称 → 解析器
     * @param names           配置的 resolver 名列表
     * @param context         请求上下文
     * @return 组合键
     */
    public Optional<String> compose(
            Map<String, RateLimitKeyResolver> resolversByName,
            List<String> names,
            RateLimitContext context) {
        if (resolversByName == null || names == null || names.isEmpty() || context == null) {
            return Optional.empty();
        }
        List<String> parts = new ArrayList<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            RateLimitKeyResolver resolver = resolversByName.get(name);
            if (resolver == null) {
                continue;
            }
            String part = resolver.resolve(context);
            if (part != null && !part.isBlank()) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(":", parts));
    }
}
