package com.mdyaipay.gateway.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 本地/联调优先走 user 内网 HTTP 查凭证，避免 Dubbo 注册 IP 不一致导致 gateway 启动失败。
 */
@Configuration
public class GatewayOpenApiCredentialConfiguration {

    @Bean
    @Primary
    OpenApiCredentialResolver httpOpenApiCredentialResolver(
            @Value("${mdyaipay.gateway.user-base-url:http://127.0.0.1:8082}") String userBaseUrl,
            ObjectMapper json) {
        return new HttpOpenApiCredentialResolver(userBaseUrl, json);
    }
}
