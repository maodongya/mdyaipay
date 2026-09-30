package com.mdyaipay.tools.sentinel.observe;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Sentinel 本机限流观测：{@code mdyaipay.sentinel.entries} Counter，供 Prometheus / Grafana 与 Dashboard 资源名对齐。
 * <p>
 * 无 {@link MeterRegistry} 时仅打 blocked 的 WARN 日志。
 */
public final class SentinelLocalMetrics {

    /** 指标名（Micrometer 导出为 {@code mdyaipay_sentinel_entries_total}）。 */
    static final String METRIC_ENTRIES = "mdyaipay.sentinel.entries";

    /** {@code SphU.entry} 成功。 */
    public static final String OUTCOME_PASSED = "passed";

    /** {@link com.alibaba.csp.sentinel.slots.block.BlockException}。 */
    public static final String OUTCOME_BLOCKED = "blocked";

    /** Dubbo Provider 入站。 */
    public static final String CHANNEL_DUBBO_PROVIDER = "dubbo_provider";

    /** Dubbo Consumer 出站。 */
    public static final String CHANNEL_DUBBO_CONSUMER = "dubbo_consumer";

    /** Spring Cloud Gateway HTTP。 */
    public static final String CHANNEL_HTTP = "http";

    private static final Logger log = LoggerFactory.getLogger(SentinelLocalMetrics.class);

    private final MeterRegistry registry;

    /**
     * @param registry Micrometer 注册表，可为 null
     */
    public SentinelLocalMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 记录一次 Sentinel entry 结果。
     * <p>
     * 幂等：是（每次调用独立累加）。
     *
     * @param resource Sentinel 资源名，如 {@code local:payment-gateway-facade-provider}
     * @param outcome  {@link #OUTCOME_PASSED} 或 {@link #OUTCOME_BLOCKED}
     * @param channel  {@link #CHANNEL_DUBBO_PROVIDER} 等
     */
    public void record(String resource, String outcome, String channel) {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(channel, "channel");
        if (registry != null) {
            registry.counter(
                            METRIC_ENTRIES,
                            "resource", resource,
                            "outcome", outcome,
                            "channel", channel)
                    .increment();
        }
        if (OUTCOME_BLOCKED.equals(outcome)) {
            log.warn("event=sentinel_local_blocked resource={} channel={}", resource, channel);
        }
    }
}
