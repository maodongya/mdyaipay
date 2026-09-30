package com.mdyaipay.finance.config;

import com.mdyaipay.finance.repository.mybatis.FinanceSchemaInitializer;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 财务模块 Spring 装配：MyBatis 扫描、DDL 初始化、雪花 ID。
 */
@Configuration
@EnableConfigurationProperties(FinanceProperties.class)
@MapperScan("com.mdyaipay.finance.repository.mybatis.mapper")
public class FinanceConfiguration {

    /**
     * 财务批次主键生成器。
     */
    @Bean
    SnowflakeIdGenerator financeSnowflakeIdGenerator(FinanceProperties properties) {
        FinanceProperties.Id id = properties.getId();
        return new SnowflakeIdGenerator(id.getWorkerId(), id.getDatacenterId());
    }

    /**
     * 启动时建对账表。{@code finance.jdbc.init-schema=false} 时不执行。
     */
    @Bean
    @ConditionalOnProperty(name = "finance.jdbc.init-schema", havingValue = "true")
    ApplicationRunner financeSchemaInitializer(DataSource dataSource, FinanceProperties properties) {
        return args -> FinanceSchemaInitializer.apply(dataSource, properties.getJdbc().isResetSchema());
    }
}
