package com.mdyaipay.tools.trace.autoconfigure;

import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Trace 公共 Bean：配置属性与 MDC 支持。
 */
@AutoConfiguration
@EnableConfigurationProperties(TraceProperties.class)
@ConditionalOnProperty(prefix = "mdyaipay.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceAutoConfiguration {

    @Bean
    TraceMdcSupport traceMdcSupport(TraceProperties properties) {
        return new TraceMdcSupport(properties);
    }
}
