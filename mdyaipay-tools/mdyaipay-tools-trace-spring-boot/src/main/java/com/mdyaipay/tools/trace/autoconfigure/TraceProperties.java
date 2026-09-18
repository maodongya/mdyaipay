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
}
