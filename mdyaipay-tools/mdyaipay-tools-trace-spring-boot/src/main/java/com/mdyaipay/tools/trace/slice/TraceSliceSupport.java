package com.mdyaipay.tools.trace.slice;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceSnapshot;
import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Optional;

/**
 * {@link TraceSliceAspect} 各 {@code @Around} Advice 的共享实现。
 * <p>
 * 在已有 {@link TraceContext} 时，为当前 Bean 方法调用切换 {@link TraceSnapshot#childSpan()}，
 * 并同步 MDC；方法返回或抛错后在 {@code finally} 中输出切片全链路日志并恢复外层快照。
 * <b>不负责</b>创建根 trace、跨进程传播或 Span 上报。
 */
public final class TraceSliceSupport {

    /** 工具类，禁止实例化。 */
    private TraceSliceSupport() {
    }

    /**
     * 在活跃 Trace 下以子 span 执行连接点；无上下文时不改线程状态，直接 {@link ProceedingJoinPoint#proceed()}。
     *
     * <p>前置条件：{@code joinPoint}、{@code mdcSupport} 非 null。
     * <p>幂等：嵌套切片通过 {@link TraceContext#bind} / {@link TraceContext#restore} 栈式恢复，traceId 不变。
     * <p>副作用：有上下文时临时改写 {@link TraceContext} 与 MDC 中的 spanId，{@code finally} 中写日志并恢复。
     *
     * @param joinPoint      当前切面拦截的连接点
     * @param mdcSupport     与入口 Filter 共用的 MDC 写入器
     * @param logOnComplete  是否在 {@code finally} 中输出 {@link TraceSliceCompletionLog} 全链路字段
     * @return {@code proceed()} 的返回值
     */
    public static Object aroundWithChildSpan(
            ProceedingJoinPoint joinPoint, TraceMdcSupport mdcSupport, boolean logOnComplete) throws Throwable {
        Optional<TraceSnapshot> current = TraceContext.current();
        if (current.isEmpty()) {
            return joinPoint.proceed();
        }

        /* 功能块：进入本层切片 — 生成子 span 并绑定上下文与 MDC，保证日志与出站头使用本段 spanId */
        TraceSnapshot child = current.get().childSpan();
        Optional<TraceSnapshot> previous = TraceContext.bind(child);
        mdcSupport.put(child);
        long startNanos = System.nanoTime();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            /* 功能块：切片结束 — 打全链路日志后再恢复上下文，避免日志里 span 与 MDC 不一致 */
            TraceSliceCompletionLog.logIfEnabled(logOnComplete, child, joinPoint, startNanos, error);
            TraceContext.restore(previous);
            mdcSupport.restore(previous);
        }
    }
}
