package com.mdyaipay.tools.timetrace;

/**
 * {@link TimeTrace} 入口一次调用结束后的报告 sink。
 * <p>
 * 通过 {@link TimeTraceSupport#setListener(TimeTraceListener)} 替换默认实现（标准输出）。
 */
@FunctionalInterface
public interface TimeTraceListener {

    /**
     * 默认 Listener：将 {@link TimeTraceReportFormatter} 结果打印到标准输出。
     */
    TimeTraceListener LOG_TO_STDOUT = report -> System.out.println(TimeTraceReportFormatter.format(report));

    /**
     * 入口追踪结束且满足 {@link TimeTrace#reportOnComplete()} 与耗时门槛时调用。
     *
     * @param report 含入口标签、总耗时、调用树；失败时 {@link TimeTraceReport#getError()} 非 null
     */
    void onComplete(TimeTraceReport report);
}
