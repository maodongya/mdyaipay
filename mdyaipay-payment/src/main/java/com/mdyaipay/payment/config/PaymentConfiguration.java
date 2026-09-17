package com.mdyaipay.payment.config;

import com.mdyaipay.payment.domain.collect.PaymentGateway;
import com.mdyaipay.payment.domain.payout.PayoutGateway;
import com.mdyaipay.payment.domain.withhold.WithholdGateway;
import com.mdyaipay.payment.gateway.MockPaymentGateway;
import com.mdyaipay.payment.gateway.MockPayoutGateway;
import com.mdyaipay.payment.gateway.MockWithholdGateway;
import com.mdyaipay.payment.repository.PaymentSchemaInitializer;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * payment 模块 Spring 装配：Mock 渠道网关、MyBatis、雪花 ID、可选 schema 初始化。
 */
@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
@MapperScan("com.mdyaipay.payment.mybatis.mapper")
public class PaymentConfiguration {

    @Bean
    SnowflakeIdGenerator paymentSnowflakeIdGenerator(PaymentProperties properties) {
        PaymentProperties.Id id = properties.getId();
        return new SnowflakeIdGenerator(id.getWorkerId(), id.getDatacenterId());
    }

    @Bean
    @ConditionalOnProperty(name = "payment.jdbc.init-schema", havingValue = "true")
    ApplicationRunner paymentSchemaInitializer(DataSource dataSource) {
        return args -> PaymentSchemaInitializer.apply(dataSource);
    }

    @Bean
    PaymentGateway paymentGateway() {
        return new MockPaymentGateway();
    }

    @Bean
    WithholdGateway withholdGateway() {
        return new MockWithholdGateway();
    }

    @Bean
    PayoutGateway payoutGateway() {
        return new MockPayoutGateway();
    }
}
