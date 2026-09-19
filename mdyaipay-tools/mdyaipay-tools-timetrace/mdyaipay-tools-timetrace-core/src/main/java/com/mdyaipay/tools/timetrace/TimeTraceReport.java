package com.mdyaipay.tools.timetrace;

/**
 * 一次 {@link TimeTrace} 入口执行结束后的只读汇总。
 * <p>
 * 由 {@link TimeTraceAspect} 在入口 {@code finally} 中组装；经 {@link TimeTraceListener} 交给日志或监控。
 */
public final class TimeTraceReport {

    private final String label;
    private final TimeTraceNode root;
    private final long totalNanos;
    private final Throwable error;

    /**
     * @param label       来自 {@link TimeTrace#value()} 或连接点签名
     * @param root        入口方法为根的节点（Spring AOP 下通常为单根）
     * @param totalNanos  入口 wall-clock 纳秒
     * @param error       入口抛出的异常；正常结束时为 null
     */
    TimeTraceReport(String label, TimeTraceNode root, long totalNanos, Throwable error) {
        this.label = label;
        this.root = root;
        this.totalNanos = totalNanos;
        this.error = error;
    }

    /** 报告标题，与日志检索字段一致。 */
    public String getLabel() {
        return label;
    }

    /** 调用树根节点，对应入口方法一次 execution。 */
    public TimeTraceNode getRoot() {
        return root;
    }

    /** 入口方法总耗时（纳秒）。 */
    public long getTotalNanos() {
        return totalNanos;
    }

    /**
     * 入口异常；非 null 时 {@link #isSuccess()} 为 false，且通常仍会计入报告（不受耗时阈值限制）。
     */
    public Throwable getError() {
        return error;
    }

    /** 入口是否正常返回（未抛错到切面）。 */
    public boolean isSuccess() {
        return error == null;
    }
}
