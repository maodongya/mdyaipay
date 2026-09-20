package com.mdyaipay.tools.ratelimit.nacos.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nacos 限流配置接入：{@code mdyaipay.ratelimit.nacos.*}。
 */
@ConfigurationProperties(prefix = "mdyaipay.ratelimit.nacos")
public class RateLimitNacosProperties {

    private boolean enabled = false;
    /** 如 {@code 127.0.0.1:8848} 或 {@code nacos:8848}。 */
    private String serverAddr = "127.0.0.1:8848";
    /** 留空则使用 {@code {spring.application.name}-ratelimit.yaml}。 */
    private String dataId;
    private String group = "DEFAULT_GROUP";
    private String namespace = "";
    /** 拉取超时（毫秒）。 */
    private long timeoutMs = 5000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
