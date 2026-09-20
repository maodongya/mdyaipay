package com.mdyaipay.tools.sentinel.autoconfigure;

import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.ratelimit.match.RateLimitDubboRuleMatcher;
import com.mdyaipay.tools.sentinel.dubbo.SentinelLocalDubboConsumerFilter;
import com.mdyaipay.tools.sentinel.dubbo.SentinelLocalDubboFilterSupport;
import com.mdyaipay.tools.sentinel.dubbo.SentinelLocalDubboProviderFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Dubbo 本机 Sentinel Filter Bean。
 */
@AutoConfiguration(after = SentinelAutoConfiguration.class)
@ConditionalOnClass(name = "org.apache.dubbo.rpc.Filter")
@ConditionalOnProperty(prefix = "mdyaipay.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SentinelDubboAutoConfiguration {

    /**
     * Dubbo Sentinel 支持类。
     */
    @Bean
    @ConditionalOnBean(RateLimitDubboRuleMatcher.class)
    SentinelLocalDubboFilterSupport sentinelLocalDubboFilterSupport(
            MdyaipaySentinelProperties sentinelProperties,
            RateLimitProperties rateLimitProperties,
            RateLimitDubboRuleMatcher ruleMatcher) {
        return new SentinelLocalDubboFilterSupport(sentinelProperties, rateLimitProperties, ruleMatcher);
    }

    /**
     * Provider Filter。
     */
    @Bean
    @ConditionalOnBean(SentinelLocalDubboFilterSupport.class)
    SentinelLocalDubboProviderFilter sentinelLocalDubboProviderFilter(SentinelLocalDubboFilterSupport support) {
        return new SentinelLocalDubboProviderFilter(support);
    }

    /**
     * Consumer Filter。
     */
    @Bean
    @ConditionalOnBean(SentinelLocalDubboFilterSupport.class)
    SentinelLocalDubboConsumerFilter sentinelLocalDubboConsumerFilter(SentinelLocalDubboFilterSupport support) {
        return new SentinelLocalDubboConsumerFilter(support);
    }
}
