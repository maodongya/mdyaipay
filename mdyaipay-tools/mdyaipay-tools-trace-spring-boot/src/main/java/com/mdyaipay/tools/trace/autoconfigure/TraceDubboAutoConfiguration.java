package com.mdyaipay.tools.trace.autoconfigure;

import com.mdyaipay.tools.trace.dubbo.TraceDubboConsumerFilter;
import com.mdyaipay.tools.trace.dubbo.TraceDubboProviderFilter;
import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 *  classpath 存在 Dubbo {@link org.apache.dubbo.rpc.Filter} 时注册 Trace 过滤器 Bean。
 */
@AutoConfiguration(after = TraceAutoConfiguration.class)
@ConditionalOnClass(name = "org.apache.dubbo.rpc.Filter")
@ConditionalOnProperty(prefix = "mdyaipay.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "mdyaipay.trace.dubbo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceDubboAutoConfiguration {

    /**
     * Consumer 出站注入 Filter；{@code @Activate} 由 Dubbo 扩展加载。
     */
    @Bean
    TraceDubboConsumerFilter traceDubboConsumerFilter() {
        return new TraceDubboConsumerFilter();
    }

    /**
     * Provider 入站续链 Filter。
     */
    @Bean
    TraceDubboProviderFilter traceDubboProviderFilter(TraceMdcSupport mdcSupport) {
        return new TraceDubboProviderFilter(mdcSupport);
    }
}
