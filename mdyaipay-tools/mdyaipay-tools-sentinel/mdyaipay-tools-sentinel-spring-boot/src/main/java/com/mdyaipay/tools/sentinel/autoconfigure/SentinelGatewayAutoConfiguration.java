package com.mdyaipay.tools.sentinel.autoconfigure;

import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.ratelimit.gateway.RateLimitDeniedWriter;
import com.mdyaipay.tools.ratelimit.match.RateLimitRuleMatcher;
import com.mdyaipay.tools.sentinel.gateway.SentinelLocalGatewayFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Gateway 本机 Sentinel {@link SentinelLocalGatewayFilter}。
 */
@AutoConfiguration(after = SentinelAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@ConditionalOnProperty(prefix = "mdyaipay.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SentinelGatewayAutoConfiguration {

    /**
     * 本机限流 Filter。
     */
    @Bean
    @ConditionalOnBean(RateLimitDeniedWriter.class)
    SentinelLocalGatewayFilter sentinelLocalGatewayFilter(
            MdyaipaySentinelProperties sentinelProperties,
            RateLimitProperties rateLimitProperties,
            RateLimitRuleMatcher ruleMatcher,
            RateLimitDeniedWriter deniedWriter) {
        return new SentinelLocalGatewayFilter(sentinelProperties, rateLimitProperties, ruleMatcher, deniedWriter);
    }
}
