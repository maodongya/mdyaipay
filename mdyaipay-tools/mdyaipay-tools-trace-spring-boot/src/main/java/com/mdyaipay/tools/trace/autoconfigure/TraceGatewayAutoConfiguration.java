package com.mdyaipay.tools.trace.autoconfigure;

import com.mdyaipay.tools.trace.http.gateway.TraceGatewayGlobalFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 存在 Spring Cloud Gateway 时注册 {@link TraceGatewayGlobalFilter}。
 */
@AutoConfiguration(after = TraceAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@ConditionalOnProperty(prefix = "mdyaipay.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceGatewayAutoConfiguration {

    @Bean
    TraceGatewayGlobalFilter traceGatewayGlobalFilter(TraceProperties properties) {
        return new TraceGatewayGlobalFilter(properties);
    }
}
