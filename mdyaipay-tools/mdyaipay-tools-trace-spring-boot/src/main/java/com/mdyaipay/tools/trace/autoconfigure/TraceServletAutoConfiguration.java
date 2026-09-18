package com.mdyaipay.tools.trace.autoconfigure;

import com.mdyaipay.tools.trace.http.servlet.ServletTraceFilter;
import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Servlet 栈注册 {@link ServletTraceFilter}。
 */
@AutoConfiguration(after = TraceAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@ConditionalOnProperty(prefix = "mdyaipay.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceServletAutoConfiguration {

    @Bean
    FilterRegistrationBean<ServletTraceFilter> servletTraceFilterRegistration(TraceMdcSupport mdcSupport) {
        FilterRegistrationBean<ServletTraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ServletTraceFilter(mdcSupport));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
