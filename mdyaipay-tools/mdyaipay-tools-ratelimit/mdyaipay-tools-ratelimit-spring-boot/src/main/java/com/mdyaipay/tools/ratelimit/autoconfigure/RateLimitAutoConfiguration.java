package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.mdyaipay.tools.ratelimit.RateLimitKeyResolver;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.match.RateLimitKeyComposer;
import com.mdyaipay.tools.ratelimit.match.RateLimitRuleMatcher;
import com.mdyaipay.tools.ratelimit.memory.InMemoryRateLimiter;
import com.mdyaipay.tools.ratelimit.resolve.ClientIpKeyResolver;
import com.mdyaipay.tools.ratelimit.resolve.DubboMethodKeyResolver;
import com.mdyaipay.tools.ratelimit.resolve.DubboServiceKeyResolver;
import com.mdyaipay.tools.ratelimit.resolve.MerchantAppKeyResolver;
import com.mdyaipay.tools.ratelimit.resolve.PathKeyResolver;
import com.mdyaipay.tools.ratelimit.resolve.RouteIdKeyResolver;
import com.mdyaipay.tools.ratelimit.dubbo.RateLimitDubboConsumerFilter;
import com.mdyaipay.tools.ratelimit.dubbo.RateLimitDubboProviderFilter;
import com.mdyaipay.tools.ratelimit.gateway.RateLimitGatewayFilter;
import com.mdyaipay.tools.ratelimit.observe.RateLimitMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 限流核心自动配置：Properties、内置 KeyResolver、内存/Redis {@link RateLimiter}。
 */
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "mdyaipay.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {

    /**
     * 规则匹配器。
     */
    @Bean
    @ConditionalOnMissingBean
    RateLimitRuleMatcher rateLimitRuleMatcher() {
        return new RateLimitRuleMatcher();
    }

    /**
     * 组合键工具。
     */
    @Bean
    @ConditionalOnMissingBean
    RateLimitKeyComposer rateLimitKeyComposer() {
        return new RateLimitKeyComposer();
    }

    /**
     * 内置解析器名 → 实现（与 yaml key-resolvers 对齐）。
     */
    @Bean(name = "rateLimitKeyResolvers")
    @ConditionalOnMissingBean(name = "rateLimitKeyResolvers")
    Map<String, RateLimitKeyResolver> rateLimitKeyResolvers() {
        Map<String, RateLimitKeyResolver> map = new LinkedHashMap<>();
        map.put("clientIp", new ClientIpKeyResolver());
        map.put("path", new PathKeyResolver());
        map.put("routeId", new RouteIdKeyResolver());
        map.put("merchantAppKey", new MerchantAppKeyResolver());
        map.put("dubboService", new DubboServiceKeyResolver());
        map.put("dubboMethod", new DubboMethodKeyResolver());
        return Map.copyOf(map);
    }

    /**
     * 内存后端（默认）。
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnProperty(prefix = "mdyaipay.ratelimit", name = "backend", havingValue = "memory", matchIfMissing = true)
    RateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }

    /**
     * Redis 后端（Lettuce + 全算法 Lua）。
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnProperty(prefix = "mdyaipay.ratelimit", name = "backend", havingValue = "redis")
    @ConditionalOnClass(name = "com.mdyaipay.tools.ratelimit.redis.RedisRateLimiter")
    RateLimiter redisRateLimiter(RateLimitProperties properties) {
        return new RedisRateLimiterHolder(properties.getRedis().getUri(), properties.getRedis().getTimeout());
    }

    /**
     * Redisson 后端（首版仅滑动窗口日志）。
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnProperty(prefix = "mdyaipay.ratelimit", name = "backend", havingValue = "redisson")
    @ConditionalOnClass(name = "com.mdyaipay.tools.ratelimit.redisson.RedissonSlidingWindowLogRateLimiter")
    RateLimiter redissonRateLimiter(RateLimitProperties properties) {
        return new RedissonRateLimiterHolder(properties.getRedis().getUri(), properties.getRedis().getTimeout());
    }

    /**
     * M1 限流指标（无 MeterRegistry 时仅日志）。
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitMetrics.class)
    RateLimitMetrics rateLimitMetrics(ObjectProvider<MeterRegistry> meterRegistry) {
        return new RateLimitMetrics(meterRegistry.getIfAvailable());
    }

    /**
     * 启动校验：backend 与驱动 jar 不一致时 fail-fast，避免限流静默失效。
     */
    @Bean
    RateLimitStartupValidator rateLimitStartupValidator(
            RateLimitProperties properties,
            ObjectProvider<RateLimiter> rateLimiter,
            ObjectProvider<RateLimitGatewayFilter> gatewayFilter,
            ObjectProvider<RateLimitDubboProviderFilter> dubboProviderFilter,
            ObjectProvider<RateLimitDubboConsumerFilter> dubboConsumerFilter) {
        return new RateLimitStartupValidator(
                properties, rateLimiter, gatewayFilter, dubboProviderFilter, dubboConsumerFilter);
    }
}
