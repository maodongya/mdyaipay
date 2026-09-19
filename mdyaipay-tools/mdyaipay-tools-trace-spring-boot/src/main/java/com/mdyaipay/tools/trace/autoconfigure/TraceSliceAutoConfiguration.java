package com.mdyaipay.tools.trace.autoconfigure;

import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import com.mdyaipay.tools.trace.slice.TraceSliceAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 注册 {@link TraceSliceAspect}（依赖 Spring AOP）。
 */
@AutoConfiguration(after = {TraceAutoConfiguration.class, AopAutoConfiguration.class})
@ConditionalOnClass(TraceSliceAspect.class)
@ConditionalOnProperty(prefix = "mdyaipay.trace.slice", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "mdyaipay.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceSliceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TraceSliceAspect traceSliceAspect(TraceMdcSupport mdcSupport, TraceProperties properties) {
        return new TraceSliceAspect(mdcSupport, properties);
    }
}
