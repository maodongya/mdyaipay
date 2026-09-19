package com.mdyaipay.tools.trace.http.gateway;

import com.mdyaipay.tools.trace.TraceHeaders;
import com.mdyaipay.tools.trace.TraceSnapshot;
import com.mdyaipay.tools.trace.autoconfigure.TraceProperties;
import com.mdyaipay.tools.trace.http.HttpHeadersTextMapCarrier;
import com.mdyaipay.tools.trace.http.IncomingTraceResolver;
import com.mdyaipay.tools.trace.http.TraceWebExchangeSupport;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Gateway 入口：解析 {@code traceparent}，写入 Exchange 属性，可选回写响应头。
 * <p>
 * 不在 Reactor 全链绑定 ThreadLocal；阻塞下游请用 {@link TraceWebExchangeSupport#callBlocking}。
 * 请求结束可选输出 {@link GatewayTraceEntryLog}（本服务入口 spanLevel 1）。
 * <p>
 * 顺序须为链上最先执行的 Filter 之一（默认 {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE}）：
 * 若业务 Filter 更靠前且不再 {@code chain.filter}，本 Filter 不会被调用。
 */
public class TraceGatewayGlobalFilter implements GlobalFilter, Ordered {

    private final TraceProperties.Http httpProperties;

    public TraceGatewayGlobalFilter(TraceProperties properties) {
        this.httpProperties = properties.getHttp();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        TraceSnapshot snapshot = IncomingTraceResolver.resolve(
                new HttpHeadersTextMapCarrier(exchange.getRequest().getHeaders()));
        exchange.getAttributes().put(TraceWebExchangeSupport.SNAPSHOT_ATTRIBUTE, snapshot);
        if (httpProperties.isPropagateResponseHeader()) {
            addResponseTraceParent(exchange.getResponse(), snapshot);
        }
        long startNanos = System.nanoTime();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        return chain.filter(exchange)
                .doOnError(errorRef::set)
                .doFinally(signal -> GatewayTraceEntryLog.logIfEnabled(
                        httpProperties.isLogEntryOnComplete(), snapshot, exchange, startNanos, errorRef.get()));
    }

    private void addResponseTraceParent(ServerHttpResponse response, TraceSnapshot snapshot) {
        response.getHeaders().set(TraceHeaders.TRACE_PARENT, snapshot.toTraceParentHeader());
    }

    @Override
    public int getOrder() {
        return httpProperties.getGatewayFilterOrder();
    }
}
