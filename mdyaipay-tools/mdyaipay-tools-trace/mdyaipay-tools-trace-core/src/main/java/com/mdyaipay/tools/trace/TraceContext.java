package com.mdyaipay.tools.trace;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 当前线程绑定的 {@link TraceSnapshot}。
 * <p>
 * 须在入口 {@link #bind} / {@link #run} 后于 finally 中 {@link #clear()} 或 {@link #restore}，避免线程池泄漏。
 * <b>不负责</b> Reactor Context、MDC 或 Span 上报。
 */
public final class TraceContext {

    private static final ThreadLocal<TraceSnapshot> CURRENT = new ThreadLocal<>();

    private TraceContext() {
    }

    /** 当前快照；未绑定时 empty。 */
    public static Optional<TraceSnapshot> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** 当前 traceId；未绑定时 empty。 */
    public static Optional<String> currentTraceId() {
        return current().map(TraceSnapshot::traceId);
    }

    /**
     * 绑定快照，返回嵌套 bind 前的快照（可能 empty）。
     */
    public static Optional<TraceSnapshot> bind(TraceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        TraceSnapshot previous = CURRENT.get();
        CURRENT.set(snapshot);
        return Optional.ofNullable(previous);
    }

    /** 移除绑定。 */
    public static void clear() {
        CURRENT.remove();
    }

    /** 恢复嵌套 bind；previous 为空时等价于 {@link #clear()}。 */
    public static void restore(Optional<TraceSnapshot> previous) {
        if (previous == null || previous.isEmpty()) {
            clear();
        } else {
            CURRENT.set(previous.get());
        }
    }

    /**
     * 在快照作用域内执行（Outbox Relay、单测）。
     *
     * <p>幂等：嵌套 run 互不影响，内层结束后恢复外层快照。
     */
    public static void run(TraceSnapshot snapshot, Runnable action) {
        Objects.requireNonNull(action, "action");
        Optional<TraceSnapshot> previous = bind(snapshot);
        try {
            action.run();
        } finally {
            restore(previous);
        }
    }

    /**
     * 在快照作用域内执行并返回结果。
     */
    public static <T> T run(TraceSnapshot snapshot, Callable<T> action) throws Exception {
        Objects.requireNonNull(action, "action");
        Optional<TraceSnapshot> previous = bind(snapshot);
        try {
            return action.call();
        } finally {
            restore(previous);
        }
    }

    /**
     * 捕获当前上下文并生成本段子 span，供跨线程传递；无上下文时 empty。
     */
    public static Optional<TraceSnapshot> captureForChildThread() {
        return current().map(TraceSnapshot::childSpan);
    }
}
