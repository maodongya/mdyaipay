package com.mdyaipay.tools.timetrace.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code mdyaipay.timetrace.*} 配置项。
 */
@ConfigurationProperties(prefix = "mdyaipay.timetrace")
public class TimeTraceProperties {

    /**
     * 是否注册 {@link com.mdyaipay.tools.timetrace.TimeTraceAspect} Bean。
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
