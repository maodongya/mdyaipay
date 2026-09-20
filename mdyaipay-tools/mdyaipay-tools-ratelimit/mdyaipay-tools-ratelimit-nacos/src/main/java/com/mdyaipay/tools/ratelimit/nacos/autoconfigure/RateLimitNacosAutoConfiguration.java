package com.mdyaipay.tools.ratelimit.nacos.autoconfigure;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitAutoConfiguration;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.ratelimit.nacos.RateLimitNacosConfigRefresher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * 启用 Nacos 动态限流配置（覆盖/补充本地 yaml）。
 */
@AutoConfiguration(after = RateLimitAutoConfiguration.class)
@EnableConfigurationProperties(RateLimitNacosProperties.class)
@ConditionalOnProperty(prefix = "mdyaipay.ratelimit.nacos", name = "enabled", havingValue = "true")
public class RateLimitNacosAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RateLimitNacosAutoConfiguration.class);

    /**
     * Nacos 限流配置刷新器。
     */
    @Bean(destroyMethod = "shutdown")
    RateLimitNacosConfigRefresher rateLimitNacosConfigRefresher(
            RateLimitNacosProperties nacosProperties,
            RateLimitProperties rateLimitProperties,
            @Value("${spring.application.name:mdyaipay-app}") String appName)
            throws NacosException {
        String dataId = nacosProperties.getDataId();
        if (!StringUtils.hasText(dataId)) {
            dataId = appName + "-ratelimit.yaml";
        }
        Properties client = new Properties();
        client.setProperty("serverAddr", nacosProperties.getServerAddr());
        if (StringUtils.hasText(nacosProperties.getNamespace())) {
            client.setProperty("namespace", nacosProperties.getNamespace());
        }
        ConfigService configService = NacosFactory.createConfigService(client);
        RateLimitNacosConfigRefresher refresher =
                new RateLimitNacosConfigRefresher(nacosProperties, rateLimitProperties, dataId);
        refresher.start(configService);
        return refresher;
    }
}
