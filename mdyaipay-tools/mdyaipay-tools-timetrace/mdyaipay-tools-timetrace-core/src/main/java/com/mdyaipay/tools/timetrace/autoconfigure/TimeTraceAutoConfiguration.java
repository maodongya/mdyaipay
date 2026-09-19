package com.mdyaipay.tools.timetrace.autoconfigure;

import com.mdyaipay.tools.timetrace.TimeTraceAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 在引入 {@code spring-boot-starter-aop}（或等价 AOP 自动配置）时注册 {@link TimeTraceAspect}。
 */
@AutoConfiguration(after = AopAutoConfiguration.class)
@ConditionalOnClass(TimeTraceAspect.class)
@EnableConfigurationProperties(TimeTraceProperties.class)
@ConditionalOnProperty(prefix = "mdyaipay.timetrace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TimeTraceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TimeTraceAspect timeTraceAspect() {
        return new TimeTraceAspect();
    }

    @Bean
    TimeTraceListenerConfigurer timeTraceListenerConfigurer(TimeTraceProperties properties) {
        return new TimeTraceListenerConfigurer(properties);
    }
}
