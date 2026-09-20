package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.dubbo.RateLimitDubboConsumerFilter;
import com.mdyaipay.tools.ratelimit.dubbo.RateLimitDubboFilterSupport;
import com.mdyaipay.tools.ratelimit.dubbo.RateLimitDubboProviderFilter;
import com.mdyaipay.tools.ratelimit.match.RateLimitDubboRuleMatcher;
import com.mdyaipay.tools.ratelimit.match.RateLimitKeyComposer;
import com.mdyaipay.tools.ratelimit.observe.RateLimitMetrics;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * Dubbo {@link org.apache.dubbo.rpc.Filter} 限流：Provider 入站与 Consumer 出站。
 */
@AutoConfiguration(after = RateLimitAutoConfiguration.class)
@ConditionalOnClass(name = "org.apache.dubbo.rpc.Filter")
@ConditionalOnProperty(prefix = "mdyaipay.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "mdyaipay.ratelimit.dubbo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitDubboAutoConfiguration {

    /**
     * Dubbo 规则匹配。
     */
    @Bean
    @ConditionalOnProperty(prefix = "mdyaipay.ratelimit.dubbo", name = "enabled", havingValue = "true", matchIfMissing = true)
    RateLimitDubboRuleMatcher rateLimitDubboRuleMatcher() {
        return new RateLimitDubboRuleMatcher();
    }

    /**
     * Provider / Consumer 共用支持类。
     */
    @Bean
    RateLimitDubboFilterSupport rateLimitDubboFilterSupport(
            RateLimitProperties properties,
            RateLimiter rateLimiter,
            RateLimitDubboRuleMatcher dubboRuleMatcher,
            RateLimitKeyComposer keyComposer,
            @Qualifier("rateLimitKeyResolvers") Map<String, RateLimitKeyResolver> resolvers,
            RateLimitMetrics metrics) {
        return new RateLimitDubboFilterSupport(
                properties, rateLimiter, dubboRuleMatcher, keyComposer, resolvers, metrics);
    }

    /**
     * Provider 侧 Filter Bean（{@code @Activate} 由 Dubbo 加载）。
     */
    @Bean
    RateLimitDubboProviderFilter rateLimitDubboProviderFilter(RateLimitDubboFilterSupport support) {
        return new RateLimitDubboProviderFilter(support);
    }

    /**
     * Consumer 侧 Filter Bean。
     */
    @Bean
    RateLimitDubboConsumerFilter rateLimitDubboConsumerFilter(RateLimitDubboFilterSupport support) {
        return new RateLimitDubboConsumerFilter(support);
    }
}
