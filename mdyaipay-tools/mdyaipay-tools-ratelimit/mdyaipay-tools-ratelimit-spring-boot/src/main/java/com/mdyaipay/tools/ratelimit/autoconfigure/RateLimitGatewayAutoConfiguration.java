package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.gateway.RateLimitDeniedWriter;
import com.mdyaipay.tools.ratelimit.gateway.RateLimitGatewayFilter;
import com.mdyaipay.tools.ratelimit.observe.RateLimitMetrics;
import com.mdyaipay.tools.ratelimit.match.RateLimitKeyComposer;
import com.mdyaipay.tools.ratelimit.match.RateLimitRuleMatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * 存在 Spring Cloud Gateway 时注册 {@link RateLimitGatewayFilter}。
 */
@AutoConfiguration(after = RateLimitAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@ConditionalOnProperty(prefix = "mdyaipay.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitGatewayAutoConfiguration {

    /**
     * 拒绝响应写入器。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.mdyaipay.tools.model.ApiResponse")
    RateLimitDeniedWriter rateLimitDeniedWriter(ObjectMapper objectMapper) {
        return new RateLimitDeniedWriter(objectMapper);
    }

    /**
     * Gateway 全局限流 Filter。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({RateLimiter.class, RateLimitDeniedWriter.class})
    RateLimitGatewayFilter rateLimitGatewayFilter(
            RateLimitProperties properties,
            RateLimiter rateLimiter,
            RateLimitRuleMatcher ruleMatcher,
            RateLimitKeyComposer keyComposer,
            Map<String, RateLimitKeyResolver> rateLimitKeyResolvers,
            RateLimitDeniedWriter deniedWriter,
            RateLimitMetrics rateLimitMetrics) {
        return new RateLimitGatewayFilter(
                properties,
                rateLimiter,
                ruleMatcher,
                keyComposer,
                rateLimitKeyResolvers,
                deniedWriter,
                rateLimitMetrics);
    }
}
