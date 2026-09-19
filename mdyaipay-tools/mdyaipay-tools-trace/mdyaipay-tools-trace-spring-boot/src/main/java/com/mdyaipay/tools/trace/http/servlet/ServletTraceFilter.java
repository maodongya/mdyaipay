package com.mdyaipay.tools.trace.http.servlet;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceSnapshot;
import com.mdyaipay.tools.trace.http.IncomingTraceResolver;
import com.mdyaipay.tools.trace.http.ServletRequestTextMapCarrier;
import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet 入口：解析/新建 Trace，绑定 {@link TraceContext} 与 MDC，请求结束清理。
 * <p>
 * {@code finally} 中可选输出入口段 {@link ServletTraceEntryLog}（本服务入口恒为 spanLevel 1），与 {@link com.mdyaipay.tools.trace.slice.TraceSlice} 方法内切片衔接。
 */
public class ServletTraceFilter extends OncePerRequestFilter {

    private final TraceMdcSupport mdcSupport;
    private final boolean logEntryOnComplete;

    /**
     * @param mdcSupport           MDC 写入器
     * @param logEntryOnComplete   是否在请求结束时写 {@link ServletTraceEntryLog}
     */
    public ServletTraceFilter(TraceMdcSupport mdcSupport, boolean logEntryOnComplete) {
        this.mdcSupport = mdcSupport;
        this.logEntryOnComplete = logEntryOnComplete;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        TraceSnapshot snapshot = IncomingTraceResolver.resolve(new ServletRequestTextMapCarrier(request));
        Optional<TraceSnapshot> previous = TraceContext.bind(snapshot);
        mdcSupport.put(snapshot);
        long startNanos = System.nanoTime();
        Throwable error = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            /* 功能块：入口段结束 — 先打 spanLevel 1/2 日志，再清理上下文，避免与 TraceSlice 层级脱节 */
            ServletTraceEntryLog.logIfEnabled(logEntryOnComplete, snapshot, request, startNanos, error);
            mdcSupport.clear();
            TraceContext.restore(previous);
        }
    }
}
