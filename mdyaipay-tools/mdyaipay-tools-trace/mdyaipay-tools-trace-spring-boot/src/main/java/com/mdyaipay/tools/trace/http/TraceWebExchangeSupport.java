package com.mdyaipay.tools.trace.http;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceSnapshot;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Gateway WebFlux 场景：从 Exchange 取快照并在阻塞线程中恢复 {@link TraceContext}。
 */
public final class TraceWebExchangeSupport {

    /** {@link ServerWebExchange#getAttributes()} 中存放 {@link TraceSnapshot} 的键。 */
    public static final String SNAPSHOT_ATTRIBUTE = TraceWebExchangeSupport.class.getName() + ".snapshot";

    private TraceWebExchangeSupport() {
    }

    /**
     * 读取 GlobalFilter 写入的快照；缺失时新起 trace。
     */
    public static TraceSnapshot snapshotFrom(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(SNAPSHOT_ATTRIBUTE);
        if (value instanceof TraceSnapshot snapshot) {
            return snapshot;
        }
        return TraceSnapshot.startNew();
    }

    /**
     * 在 boundedElastic 上执行阻塞逻辑，并绑定 Exchange 中的 Trace。
     */
    public static <T> Mono<T> callBlocking(ServerWebExchange exchange, Callable<T> action) {
        TraceSnapshot snapshot = snapshotFrom(exchange);
        return Mono.fromCallable(() -> TraceContext.run(snapshot, action))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
