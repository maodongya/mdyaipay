package com.mdyaipay.tools.timetrace;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 当前线程上的 {@link TimeTrace} 会话与调用栈。
 * <p>
 * 由 {@link TimeTraceEntrySupport} 在入口 advice 中 {@link #begin} / {@link #end}；
 * {@link #enterFrame} / {@link #leaveFrame} 维护入口根节点（报告树预留扩展）。
 * <b>不负责</b>跨线程传播。
 */
final class TimeTraceContext {

    private static final ThreadLocal<Session> SESSION = new ThreadLocal<>();

    private TimeTraceContext() {
    }

    /** 当前线程是否已有活跃追踪会话。 */
    static boolean isTracing() {
        return SESSION.get() != null;
    }

    /**
     * 开启会话并绑定 ThreadLocal。
     *
     * @param reportThresholdMillis 来自 {@link TimeTrace#reportThresholdMillis()}，负值按 0 处理
     */
    static Session begin(String label, TimeTraceListener listener, boolean reportOnComplete, long reportThresholdMillis) {
        Session session = new Session(label, listener, reportOnComplete, reportThresholdMillis);
        SESSION.set(session);
        return session;
    }

    /** 获取当前会话；无会话时为 null。 */
    static Session currentSession() {
        return SESSION.get();
    }

    /** 结束会话并移除 ThreadLocal，避免线程池复用泄漏。 */
    static void end() {
        SESSION.remove();
    }

    /**
     * 进入一层方法帧：创建节点并压栈，挂接到父节点或设为根。
     *
     * @return 新节点；无会话时 null（调用方应直接 proceed）
     */
    static TimeTraceNode enterFrame(String signature) {
        Session session = SESSION.get();
        if (session == null) {
            return null;
        }

        /* 功能块：挂接调用树 — 栈空则为根，否则挂到当前栈顶父节点 */
        TimeTraceNode parent = session.stack.isEmpty() ? null : session.stack.peek();
        TimeTraceNode node = new TimeTraceNode(signature);
        if (parent == null) {
            session.root = node;
        } else {
            parent.addChild(node);
        }
        session.stack.push(node);
        return node;
    }

    /**
     * 离开一层方法帧：写入耗时；仅当栈顶与当前节点一致时 pop。
     */
    static void leaveFrame(TimeTraceNode node, long durationNanos) {
        Session session = SESSION.get();
        if (session == null || node == null) {
            return;
        }

        /* 功能块：写入耗时并出栈 — 防止错序 pop 破坏树 */
        node.setDurationNanos(durationNanos);
        if (!session.stack.isEmpty() && session.stack.peek() == node) {
            session.stack.pop();
        }
    }

    /**
     * 一次入口追踪的线程内状态。
     */
    static final class Session {
        final String label;
        final TimeTraceListener listener;
        final boolean reportOnComplete;
        final long reportThresholdMillis;
        TimeTraceNode root;
        final Deque<TimeTraceNode> stack = new ArrayDeque<>();

        Session(String label, TimeTraceListener listener, boolean reportOnComplete, long reportThresholdMillis) {
            this.label = label;
            this.listener = listener;
            this.reportOnComplete = reportOnComplete;
            this.reportThresholdMillis = Math.max(0, reportThresholdMillis);
        }
    }
}
