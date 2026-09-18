package com.mdyaipay.payment.config;

import com.mdyaipay.payment.gateway.PaymentGateway;
import com.mdyaipay.payment.gateway.PayoutGateway;
import com.mdyaipay.payment.gateway.WithholdGateway;
import com.mdyaipay.payment.gateway.mock.MockPaymentGateway;
import com.mdyaipay.payment.gateway.mock.MockPayoutGateway;
import com.mdyaipay.payment.gateway.mock.MockWithholdGateway;
import com.mdyaipay.payment.repository.mybatis.PaymentSchemaInitializer;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import com.mdyaipay.tools.id.UuidV7Generator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * payment 模块 Spring 装配：Mock 渠道网关、MyBatis、雪花业务号 / UUIDv7 主键、可选 schema 初始化。
 * <p>
 * 启用声明式事务；隔离级别见 {@code spring.datasource.hikari.transaction-isolation}（READ COMMITTED）。
 */
@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(PaymentProperties.class)
@MapperScan("com.mdyaipay.payment.repository.mybatis.mapper")
public class PaymentConfiguration {

    @Bean
    SnowflakeIdGenerator paymentSnowflakeIdGenerator(PaymentProperties properties) {
        PaymentProperties.Id id = properties.getId();
        return new SnowflakeIdGenerator(id.getWorkerId(), id.getDatacenterId());
    }

    /** 订单表 {@code BINARY(16)} 主键。 */
    @Bean
    UuidV7Generator paymentOrderUuidV7Generator() {
        return new UuidV7Generator();
    }

    @Bean
    @ConditionalOnProperty(name = "payment.jdbc.init-schema", havingValue = "true")
    ApplicationRunner paymentSchemaInitializer(DataSource dataSource, PaymentProperties properties) {
        return args -> PaymentSchemaInitializer.apply(dataSource, properties.getJdbc().isResetSchema());
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
