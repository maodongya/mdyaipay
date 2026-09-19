package com.mdyaipay.tools.trace.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code mdyaipay.trace.*} 配置项。
 */
@ConfigurationProperties(prefix = "mdyaipay.trace")
public class TraceProperties {

    private boolean enabled = true;
    private final Mdc mdc = new Mdc();
    private final Http http = new Http();
    private final Dubbo dubbo = new Dubbo();
    private final Slice slice = new Slice();

    /** 是否启用 Trace 自动配置。 */
    public boolean isEnabled() {
        return enabled;
    }

    /** 设置是否启用 Trace 自动配置。 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Mdc getMdc() {
        return mdc;
    }

    public Http getHttp() {
        return http;
    }

    /** Dubbo Filter 相关配置。 */
    public Dubbo getDubbo() {
        return dubbo;
    }

    /** 单服务内 Trace 切片（Spring AOP）配置。 */
    public Slice getSlice() {
        return slice;
    }

    /** MDC 键名与开关。 */
    public static class Mdc {

        private boolean enabled = true;
        private String traceIdKey = "traceId";
        private String spanIdKey = "spanId";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTraceIdKey() {
            return traceIdKey;
        }

        public void setTraceIdKey(String traceIdKey) {
            this.traceIdKey = traceIdKey;
        }

        public String getSpanIdKey() {
            return spanIdKey;
        }

        public void setSpanIdKey(String spanIdKey) {
            this.spanIdKey = spanIdKey;
        }
    }

    /** HTTP 入口/出站相关配置。 */
    public static class Http {

        private int gatewayFilterOrder = -1000;
        private boolean propagateResponseHeader = true;
        private boolean logEntryOnComplete = true;

        public int getGatewayFilterOrder() {
            return gatewayFilterOrder;
        }

        public void setGatewayFilterOrder(int gatewayFilterOrder) {
            this.gatewayFilterOrder = gatewayFilterOrder;
        }

        public boolean isPropagateResponseHeader() {
            return propagateResponseHeader;
        }

        public void setPropagateResponseHeader(boolean propagateResponseHeader) {
            this.propagateResponseHeader = propagateResponseHeader;
        }

        /**
         * Servlet {@link com.mdyaipay.tools.trace.http.servlet.ServletTraceFilter} 结束时是否写
         * Logger {@code com.mdyaipay.tools.trace.entry.report}。
         */
        public boolean isLogEntryOnComplete() {
            return logEntryOnComplete;
        }

        public void setLogEntryOnComplete(boolean logEntryOnComplete) {
            this.logEntryOnComplete = logEntryOnComplete;
        }
    }

    /** Dubbo Consumer/Provider Filter 开关。 */
    public static class Dubbo {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** 单服务内子 span 切片：需 {@code spring-boot-starter-aop} 与 {@link com.mdyaipay.tools.trace.slice.TraceSliceAspect}。 */
    public static class Slice {

        private boolean enabled = true;
        private boolean nestedSpringBeans = true;
        private boolean logOnComplete = true;

        /** 是否注册切片切面。 */
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 是否对常见 Spring .stereotype Bean 的 public 方法自动嵌套切片；
         * 为 false 时仅 {@link com.mdyaipay.tools.trace.slice.TraceSlice} 标注生效。
         */
        public boolean isNestedSpringBeans() {
            return nestedSpringBeans;
        }

        public void setNestedSpringBeans(boolean nestedSpringBeans) {
            this.nestedSpringBeans = nestedSpringBeans;
        }

        /**
         * 每层切片 {@code finally} 是否输出全链路字段到 Logger {@code com.mdyaipay.tools.trace.slice.report}。
         */
        public boolean isLogOnComplete() {
            return logOnComplete;
        }

        public void setLogOnComplete(boolean logOnComplete) {
            this.logOnComplete = logOnComplete;
        }
    }
}
