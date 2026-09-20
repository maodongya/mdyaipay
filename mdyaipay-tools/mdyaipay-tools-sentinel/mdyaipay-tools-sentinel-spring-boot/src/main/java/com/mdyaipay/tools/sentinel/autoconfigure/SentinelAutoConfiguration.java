package com.mdyaipay.tools.sentinel.autoconfigure;

import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitAutoConfiguration;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitClusterPolicyRefresher;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sentinel 核心 Bean：控制台连接、cluster 规则桥接、本机规则种子。
 */
@AutoConfiguration(after = RateLimitAutoConfiguration.class)
@EnableConfigurationProperties(MdyaipaySentinelProperties.class)
@ConditionalOnProperty(prefix = "mdyaipay.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class SentinelAutoConfiguration {

    /**
     * Dashboard transport 配置。
     */
    @Bean
    SentinelTransportConfigurer sentinelTransportConfigurer(
            MdyaipaySentinelProperties properties, @Value("${spring.application.name:mdyaipay-app}") String appName) {
        return new SentinelTransportConfigurer(properties, appName);
    }

    /**
     * Dashboard {@code cluster:} 规则 → Redis 整体 limit。
     */
    @Bean
    SentinelClusterFlowRuleBridge sentinelClusterFlowRuleBridge(RateLimitClusterPolicyRefresher clusterRefresher) {
        SentinelClusterFlowRuleBridge bridge = new SentinelClusterFlowRuleBridge(clusterRefresher);
        bridge.registerFlowPropertyListener();
        return bridge;
    }

    /**
     * 启动种子化 {@code local:} 规则。
     */
    @Bean
    SentinelLocalRuleSeeder sentinelLocalRuleSeeder(
            MdyaipaySentinelProperties sentinelProperties, RateLimitProperties rateLimitProperties) {
        return new SentinelLocalRuleSeeder(sentinelProperties, rateLimitProperties);
    }
}
