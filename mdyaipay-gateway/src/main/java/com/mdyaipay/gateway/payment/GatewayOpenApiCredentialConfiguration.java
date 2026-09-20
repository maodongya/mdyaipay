package com.mdyaipay.gateway.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gateway→user 查凭证：默认 Dubbo；配置非空 {@code user-base-url} 时走内网 HTTP。
 */
@Configuration
public class GatewayOpenApiCredentialConfiguration {

    /**
     * @param userBaseUrl 非空则 HTTP，否则 Dubbo（Consumer 限流生效）
     */
    @Bean
    OpenApiCredentialResolver openApiCredentialResolver(
            @Value("${mdyaipay.gateway.user-base-url:}") String userBaseUrl,
            ObjectMapper json,
            DubboOpenApiCredentialResolver dubboOpenApiCredentialResolver) {
        if (userBaseUrl != null && !userBaseUrl.isBlank()) {
            return new HttpOpenApiCredentialResolver(userBaseUrl, json);
        }
        return dubboOpenApiCredentialResolver;
    }
}
