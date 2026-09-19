package com.mdyaipay.tools.trace.http.gateway;

import com.mdyaipay.tools.trace.TraceSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/**
 * Gateway HTTP 入口 Trace 段结束时的结构化日志（与 {@link com.mdyaipay.tools.trace.slice.TraceSliceCompletionLog} 互补）。
 */
final class GatewayTraceEntryLog {

    /** 与 Servlet {@link com.mdyaipay.tools.trace.http.servlet.ServletTraceEntryLog#LOGGER_NAME} 共用 Logger 名。 */
    static final String LOGGER_NAME = "com.mdyaipay.tools.trace.entry.report";

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

    private GatewayTraceEntryLog() {
    }

    /**
     * 在 GlobalFilter 请求结束时输出本 Gateway 入口 span 与耗时。
     *
     * @param logEntryOnComplete 是否输出（来自 {@code mdyaipay.trace.http.log-entry-on-complete}）
     * @param snapshot           Exchange 上的入口快照
     * @param exchange           当前 WebExchange
     * @param startNanos         进入 Filter 时的 {@link System#nanoTime()}
     * @param error              链路上抛出的异常；正常结束为 null
     */
    static void logIfEnabled(
            boolean logEntryOnComplete,
            TraceSnapshot snapshot,
            ServerWebExchange exchange,
            long startNanos,
            Throwable error) {
        if (!logEntryOnComplete || !LOG.isInfoEnabled() || snapshot == null) {
            return;
        }
        ServerHttpRequest request = exchange.getRequest();
        long durationNanos = System.nanoTime() - startNanos;
        String parent = snapshot.parentSpanId() == null ? "-" : snapshot.parentSpanId();
        String outcome = error == null ? "ok" : error.getClass().getSimpleName();
        LOG.info(
                "[TraceEntry] http={} {} traceId={} spanId={} parentSpanId={} spanLevel={} spanLevelGlobal={} serverDepthLevel={} sampled={} durationMs={} outcome={}",
                request.getMethod(),
                request.getURI().getPath(),
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
