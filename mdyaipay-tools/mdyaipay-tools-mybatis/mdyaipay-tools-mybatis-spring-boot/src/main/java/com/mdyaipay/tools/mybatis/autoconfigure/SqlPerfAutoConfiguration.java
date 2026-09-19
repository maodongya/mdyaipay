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
 * 注册 {@link SqlPerfInterceptor} 为 Spring Bean，由 {@link MybatisAutoConfiguration} 注入 MyBatis。
 * <p>
 * 勿再使用 {@link org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer} 二次 {@code addInterceptor}，
 * 否则同一条 SQL 会打两次 {@code [SqlPerf]}。
 */
@AutoConfiguration(after = MybatisAutoConfiguration.class)
@ConditionalOnClass({Interceptor.class, MybatisAutoConfiguration.class})
@EnableConfigurationProperties(SqlPerfProperties.class)
@ConditionalOnProperty(prefix = "mdyaipay.mybatis.sql-perf", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SqlPerfAutoConfiguration {

    /**
     * SQL 性能插件；MyBatis Spring Boot 会自动 {@code addInterceptor} 所有 {@link Interceptor} Bean。
     *
     * @param properties 性能日志配置
     * @return 插件实例
     */
    @Bean
    @ConditionalOnMissingBean
    SqlPerfInterceptor sqlPerfInterceptor(SqlPerfProperties properties) {
        return new SqlPerfInterceptor(properties);
    }
}
