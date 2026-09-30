package com.mdyaipay.financegateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.financegateway.integration.FinanceChannelHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * finance-gateway Spring 装配。
 */
@Configuration
@EnableConfigurationProperties(FinanceGatewayProperties.class)
public class FinanceGatewayConfiguration {

    @Bean
    FinanceChannelHttpClient financeChannelHttpClient(FinanceGatewayProperties properties, ObjectMapper objectMapper) {
        return new FinanceChannelHttpClient(properties.getChannelBaseUrl(), objectMapper);
    }
}
