package com.mdyaipay.tools.timetrace;

/**
 * {@link TimeTrace} 全局监听器配置（Spring AOP 与各模块测试共用）。
 */
public final class TimeTraceSupport {

    private static volatile TimeTraceListener globalListener = TimeTraceListener.LOG_TO_STDOUT;

    private TimeTraceSupport() {
    }

    public static TimeTraceListener globalListener() {
        return globalListener;
    }

    /** 设置全局报告回调；传入 null 时回落为 {@link TimeTraceListener#LOG_TO_STDOUT}。 */
    public static void setListener(TimeTraceListener listener) {
        globalListener = listener != null ? listener : TimeTraceListener.LOG_TO_STDOUT;
    }
}
