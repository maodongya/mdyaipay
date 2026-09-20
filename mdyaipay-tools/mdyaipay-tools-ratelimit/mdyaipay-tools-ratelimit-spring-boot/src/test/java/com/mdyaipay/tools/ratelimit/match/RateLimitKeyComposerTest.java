package com.mdyaipay.tools.ratelimit.match;

import com.mdyaipay.tools.ratelimit.RateLimitContext;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RateLimitKeyComposer} 组合键单测。
 */
class RateLimitKeyComposerTest {

    /**
     * 非 null 片段用冒号连接；全空则 empty。
     */
    @Test
    void composesNonNullSegments() {
        RateLimitKeyComposer composer = new RateLimitKeyComposer();
        Map<String, RateLimitKeyResolver> resolvers = Map.of(
                "clientIp", ctx -> ctx.clientIp(),
                "path", ctx -> ctx.path(),
                "skip", ctx -> null);
        RateLimitContext ctx = new RateLimitContext(null, "POST", "/p", "1.2.3.4", null, Map.of());

        assertEquals("1.2.3.4:/p", composer.compose(resolvers, List.of("clientIp", "path"), ctx).orElseThrow());
        assertEquals("1.2.3.4", composer.compose(resolvers, List.of("clientIp", "skip"), ctx).orElseThrow());
        assertTrue(composer.compose(resolvers, List.of("skip"), ctx).isEmpty());
    }
}
