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
 */
public class ServletTraceFilter extends OncePerRequestFilter {

    private final TraceMdcSupport mdcSupport;

    public ServletTraceFilter(TraceMdcSupport mdcSupport) {
        this.mdcSupport = mdcSupport;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        TraceSnapshot snapshot = IncomingTraceResolver.resolve(new ServletRequestTextMapCarrier(request));
        Optional<TraceSnapshot> previous = TraceContext.bind(snapshot);
        mdcSupport.put(snapshot);
        try {
            filterChain.doFilter(request, response);
        } finally {
            mdcSupport.clear();
            TraceContext.restore(previous);
        }
    }
}
