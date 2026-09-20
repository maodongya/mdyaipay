package com.mdyaipay.tools.sentinel.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sentinel 接入配置：{@code mdyaipay.sentinel.*}。
 * <p>
 * <b>不负责</b> 集群限流算法与 Redis 连接——仍由 {@code mdyaipay.ratelimit.*} 承担。
 */
@ConfigurationProperties(prefix = "mdyaipay.sentinel")
public class MdyaipaySentinelProperties {

    private boolean enabled = true;
    /** 控制台地址，如 {@code 127.0.0.1:8858} 或 {@code sentinel-dashboard:8858}。 */
    private String dashboard = "127.0.0.1:8858";
    /** 本进程与 Dashboard 通信端口（多实例需不同端口时可配环境变量）。 */
    private int transportPort = 8719;
    /** 未单独配置时，本机 QPS 默认与 yaml 规则 limit 相同。 */
    private Long defaultLocalQps;
    /** 是否在启动时用 yaml 规则种子化 {@code local:} 流控规则。 */
    private boolean seedLocalRulesFromRatelimit = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDashboard() {
        return dashboard;
    }

    public void setDashboard(String dashboard) {
        this.dashboard = dashboard;
    }

    public int getTransportPort() {
        return transportPort;
    }

    public void setTransportPort(int transportPort) {
        this.transportPort = transportPort;
    }

    public Long getDefaultLocalQps() {
        return defaultLocalQps;
    }

    public void setDefaultLocalQps(Long defaultLocalQps) {
        this.defaultLocalQps = defaultLocalQps;
    }

    public boolean isSeedLocalRulesFromRatelimit() {
        return seedLocalRulesFromRatelimit;
    }

    public void setSeedLocalRulesFromRatelimit(boolean seedLocalRulesFromRatelimit) {
        this.seedLocalRulesFromRatelimit = seedLocalRulesFromRatelimit;
    }
}
