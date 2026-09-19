package com.mdyaipay.tools.mybatis.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code mdyaipay.mybatis.sql-perf.*}：MyBatis SQL 耗时日志开关与阈值。
 * <p>
 * 需引入 {@code mdyaipay-tools-mybatis-spring-boot} 与 {@code mybatis-spring-boot-starter}。
 */
@ConfigurationProperties(prefix = "mdyaipay.mybatis.sql-perf")
public class SqlPerfProperties {

    private boolean enabled = true;
    /** 仅当耗时 ≥ 该值（毫秒）时输出；{@code 0} 表示记录每次执行。 */
    private long slowThresholdMillis = 0;
    /** 是否在日志中附带当前 {@code traceId}（需 classpath 上有 trace-core 且已绑定上下文）。 */
    private boolean includeTraceId = true;

    /** 是否启用 SQL 性能 Interceptor。 */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getSlowThresholdMillis() {
        return slowThresholdMillis;
    }

    public void setSlowThresholdMillis(long slowThresholdMillis) {
        this.slowThresholdMillis = Math.max(0, slowThresholdMillis);
    }

    public boolean isIncludeTraceId() {
        return includeTraceId;
    }

    public void setIncludeTraceId(boolean includeTraceId) {
        this.includeTraceId = includeTraceId;
    }
}
