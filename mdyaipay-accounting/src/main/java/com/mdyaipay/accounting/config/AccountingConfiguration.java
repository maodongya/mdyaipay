package com.mdyaipay.accounting.config;

import com.mdyaipay.accounting.dao.AccountingSchemaInitializer;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 账务模块 Spring 装配：MyBatis 扫描、DDL 初始化、雪花 ID。
 */
@Configuration
@EnableConfigurationProperties(AccountingProperties.class)
@MapperScan("com.mdyaipay.accounting.dao")
public class AccountingConfiguration {

    @Bean
    SnowflakeIdGenerator accountingSnowflakeIdGenerator(AccountingProperties properties) {
        AccountingProperties.Id id = properties.getId();
        return new SnowflakeIdGenerator(id.getWorkerId(), id.getDatacenterId());
    }

    @Bean
    @ConditionalOnProperty(name = "accounting.jdbc.init-schema", havingValue = "true")
    ApplicationRunner accountingSchemaInitializer(DataSource dataSource, AccountingProperties properties) {
        return args -> AccountingSchemaInitializer.apply(dataSource, properties.getJdbc().isResetSchema());
    }
}
