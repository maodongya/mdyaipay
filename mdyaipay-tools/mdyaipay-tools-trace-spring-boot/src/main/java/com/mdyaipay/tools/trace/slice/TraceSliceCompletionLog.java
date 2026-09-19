package com.mdyaipay.tools.trace.slice;

import com.mdyaipay.tools.trace.TraceSnapshot;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单服务 Trace 切片结束时的结构化日志。
 * <p>
 * 使用专用 Logger 名 {@link #LOGGER_NAME}，业务可在 Log4j2 中单独落盘（参考 TimeTrace {@code report} Logger）。
 */
final class TraceSliceCompletionLog {

    /** 切片完成日志 Logger，与 {@link com.mdyaipay.tools.timetrace.TimeTraceLog4j#REPORT_LOGGER_NAME} 用法类似。 */
    static final String LOGGER_NAME = "com.mdyaipay.tools.trace.slice.report";

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

    private TraceSliceCompletionLog() {
    }

    /**
     * 在切片 {@code finally} 中输出本层 span 与耗时；未启用 {@code logOnComplete} 或 Logger 级别不够时不写。
     *
     * @param slice       本层已结束的子 span 快照
     * @param joinPoint   当前连接点
     * @param startNanos  进入切片时的 {@link System#nanoTime()}
     * @param error       {@code proceed()} 抛出的异常；正常结束为 null
     */
    static void logIfEnabled(
            boolean logOnComplete,
            TraceSnapshot slice,
            ProceedingJoinPoint joinPoint,
            long startNanos,
            Throwable error) {
        if (!logOnComplete || !LOG.isInfoEnabled()) {
            return;
        }
        long durationNanos = System.nanoTime() - startNanos;
        String parent = slice.parentSpanId() == null ? "-" : slice.parentSpanId();
        String outcome = error == null ? "ok" : error.getClass().getSimpleName();
        LOG.info(
                "[TraceSlice] signature={} traceId={} spanId={} parentSpanId={} spanLevel={} sampled={} durationMs={} outcome={}",
                joinPoint.getSignature().toShortString(),
                slice.traceId(),
                slice.spanId(),
                parent,
                slice.spanLevel(),
                slice.sampled(),
                formatMillis(durationNanos),
                outcome);
    }

    /** 纳秒转毫秒字符串，保留三位小数。 */
    private static String formatMillis(long nanos) {
        return String.format("%.3f", nanos / 1_000_000.0);
    }
}
