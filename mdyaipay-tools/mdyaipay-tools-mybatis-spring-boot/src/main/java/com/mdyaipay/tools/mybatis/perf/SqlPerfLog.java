package com.mdyaipay.tools.mybatis.perf;

import org.apache.ibatis.mapping.MappedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyBatis 语句执行结束后的结构化性能日志。
 * <p>
 * <b>不负责</b>输出绑定参数（防 PII）；SQL 为占位符模板，最长截断 512 字符。
 */
public final class SqlPerfLog {

    /** SQL 性能专用 Logger，可在 Log4j2 中单独落盘。 */
    public static final String LOGGER_NAME = "com.mdyaipay.tools.mybatis.sql.report";

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);
    private static final int MAX_SQL_CHARS = 512;

    private SqlPerfLog() {
    }

    /**
     * 输出单条 SQL 执行摘要；未达阈值或 Logger 级别不够时不写。
     *
     * @param slowThresholdMillis {@code 0} 表示不限耗时
     * @param includeTraceId      是否附带 traceId 字段
     */
    public static void logIfNeeded(
            long slowThresholdMillis,
            boolean includeTraceId,
            MappedStatement mappedStatement,
            String sql,
            long durationMillis,
            Object result,
            Throwable error) {
        if (!LOG.isInfoEnabled()) {
            return;
        }
        if (slowThresholdMillis > 0 && durationMillis < slowThresholdMillis) {
            return;
        }
        String traceField = includeTraceId ? resolveTraceIdField() : "-";
        String outcome = error == null ? formatOutcome(result) : error.getClass().getSimpleName();
        LOG.info(
                "[SqlPerf] statementId={} durationMs={} outcome={} traceId={} sql={}",
                mappedStatement.getId(),
                formatMillis(durationMillis),
                outcome,
                traceField,
                truncateSql(sql));
    }

    /** 尝试读取 trace-core 上下文中的 traceId；无依赖或未绑定时返回 {@code -}。 */
    private static String resolveTraceIdField() {
        try {
            Class<?> contextClass = Class.forName("com.mdyaipay.tools.trace.TraceContext");
            Object optional = contextClass.getMethod("currentTraceId").invoke(null);
            if (optional instanceof java.util.Optional<?> opt && opt.isPresent()) {
                return String.valueOf(opt.get());
            }
        } catch (ReflectiveOperationException ignored) {
            // trace-core 未引入或未绑定
        }
        return "-";
    }

    private static String formatOutcome(Object result) {
        if (result == null) {
            return "ok";
        }
        if (result instanceof Integer rows) {
            return "rows=" + rows;
        }
        if (result instanceof java.util.List<?> list) {
            return "rows=" + list.size();
        }
        return "ok";
    }

    private static String truncateSql(String sql) {
        if (sql == null) {
            return "";
        }
        String normalized = sql.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_SQL_CHARS) {
            return normalized;
        }
        return normalized.substring(0, MAX_SQL_CHARS) + "...";
    }

    private static String formatMillis(long millis) {
        return String.format("%.3f", millis / 1.0);
    }
}
