package com.mdyaipay.tools.timetrace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 调用树节点：对应一次被 AOP 统计的方法 execution。
 * <p>
 * 由 {@link TimeTraceContext} 维护父子关系；{@link #getSelfNanos()} 用于区分「自身逻辑」与「子调用」耗时。
 */
public final class TimeTraceNode {

    private final String signature;
    private final List<TimeTraceNode> children = new ArrayList<>();
    private long durationNanos;
    private TimeTraceNode parent;

    /** @param signature 通常为 AspectJ {@code toShortString()} */
    TimeTraceNode(String signature) {
        this.signature = signature;
    }

    /** 方法短签名，便于日志对照源码。 */
    public String getSignature() {
        return signature;
    }

    /** 本节点 wall-clock 耗时（纳秒），含子调用。 */
    public long getDurationNanos() {
        return durationNanos;
    }

    /** 只读子节点列表，顺序与调用先后一致。 */
    public List<TimeTraceNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * 自身耗时（纳秒）≈ {@link #getDurationNanos()} 减去各子节点耗时之和。
     * 单线程同步调用链下用于观察「除子调用外的开销」；并发或缺失织入时仅供参考。
     */
    public long getSelfNanos() {
        long childSum = 0;
        for (TimeTraceNode child : children) {
            childSum += child.durationNanos;
        }
        long self = durationNanos - childSum;
        return Math.max(0, self);
    }

    void setDurationNanos(long durationNanos) {
        this.durationNanos = durationNanos;
    }

    /** 挂接子节点并返回子节点，供 {@link TimeTraceContext} 入栈。 */
    TimeTraceNode addChild(TimeTraceNode child) {
        child.parent = this;
        children.add(child);
        return child;
    }
}
