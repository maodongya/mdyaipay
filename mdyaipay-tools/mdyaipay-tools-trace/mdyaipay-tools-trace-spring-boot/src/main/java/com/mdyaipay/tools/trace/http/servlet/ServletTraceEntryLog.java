package com.mdyaipay.tools.trace.http.servlet;

import com.mdyaipay.tools.trace.TraceSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet HTTP 入口 Trace 段结束时的结构化日志（与 {@link com.mdyaipay.tools.trace.slice.TraceSliceCompletionLog} 互补）。
 */
final class ServletTraceEntryLog {

    /** 入口段完成日志 Logger；可与 {@code com.mdyaipay.tools.trace.slice.report} 一并配置落盘。 */
    static final String LOGGER_NAME = "com.mdyaipay.tools.trace.entry.report";

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

    private ServletTraceEntryLog() {
    }

    /**
     * 在 Filter {@code finally} 中输出本请求入口 span（本服务根，spanLevel 恒为 1）与耗时。
     *
     * @param logEntryOnComplete 是否输出（来自 {@code mdyaipay.trace.http.log-entry-on-complete}）
     * @param snapshot           本请求绑定的入口快照
     * @param request            当前 HTTP 请求
     * @param startNanos         进入 Filter 时的 {@link System#nanoTime()}
     * @param error              {@code doFilter} 链抛出的异常；正常结束为 null
     */
    static void logIfEnabled(
            boolean logEntryOnComplete,
            TraceSnapshot snapshot,
            HttpServletRequest request,
            long startNanos,
            Throwable error) {
        if (!logEntryOnComplete || !LOG.isInfoEnabled()) {
            return;
        }
        long durationNanos = System.nanoTime() - startNanos;
        String parent = snapshot.parentSpanId() == null ? "-" : snapshot.parentSpanId();
        String outcome = error == null ? "ok" : error.getClass().getSimpleName();
        LOG.info(
                "[TraceEntry] http={} {} traceId={} spanId={} parentSpanId={} spanLevel={} spanLevelGlobal={} serverDepthLevel={} sampled={} durationMs={} outcome={}",
                request.getMethod(),
                request.getRequestURI(),
                snapshot.traceId(),
                snapshot.spanId(),
                parent,
                snapshot.spanLevel(),
                snapshot.spanLevelGlobal(),
                snapshot.serverDepthLevel(),
                snapshot.sampled(),
                formatMillis(durationNanos),
                outcome);
    }

    /** 纳秒转毫秒字符串，保留三位小数。 */
    private static String formatMillis(long nanos) {
        return String.format("%.3f", nanos / 1_000_000.0);
    }
}
