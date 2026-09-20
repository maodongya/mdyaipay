package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.match.RateLimitKeyComposer;
import com.mdyaipay.tools.ratelimit.match.RateLimitRuleMatcher;
import com.mdyaipay.tools.ratelimit.observe.RateLimitMetrics;
import com.mdyaipay.tools.ratelimit.servlet.RateLimitServletFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * Servlet 环境且显式开启时注册 {@link RateLimitServletFilter}。
 */
@AutoConfiguration(after = RateLimitAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@ConditionalOnProperty(prefix = "mdyaipay.ratelimit.servlet", name = "enabled", havingValue = "true")
public class RateLimitServletAutoConfiguration {

    /**
     * Servlet 限流 Filter。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RateLimiter.class)
    RateLimitServletFilter rateLimitServletFilter(
            RateLimitProperties properties,
            RateLimiter rateLimiter,
            RateLimitRuleMatcher ruleMatcher,
            RateLimitKeyComposer keyComposer,
            Map<String, RateLimitKeyResolver> rateLimitKeyResolvers,
            ObjectMapper objectMapper,
            ObjectProvider<RateLimitMetrics> metrics) {
        return new RateLimitServletFilter(
                properties,
                rateLimiter,
                ruleMatcher,
                keyComposer,
                rateLimitKeyResolvers,
                objectMapper,
                metrics.getIfAvailable());
    }
}
