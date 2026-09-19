package com.mdyaipay.tools.mybatis.autoconfigure;

import com.mdyaipay.tools.mybatis.perf.SqlPerfInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 注册 {@link SqlPerfInterceptor} 到 MyBatis {@link org.apache.ibatis.session.Configuration}。
 */
@AutoConfiguration(after = MybatisAutoConfiguration.class)
@ConditionalOnClass({Interceptor.class, MybatisAutoConfiguration.class})
@EnableConfigurationProperties(SqlPerfProperties.class)
@ConditionalOnProperty(prefix = "mdyaipay.mybatis.sql-perf", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SqlPerfAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SqlPerfInterceptor sqlPerfInterceptor(SqlPerfProperties properties) {
        return new SqlPerfInterceptor(properties);
    }

    /**
     * 将性能插件挂到 MyBatis 配置（与业务 Mapper 共用同一 {@code SqlSessionFactory}）。
     */
    @Bean
    org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer sqlPerfConfigurationCustomizer(
            SqlPerfInterceptor sqlPerfInterceptor) {
        return configuration -> configuration.addInterceptor(sqlPerfInterceptor);
    }
}
