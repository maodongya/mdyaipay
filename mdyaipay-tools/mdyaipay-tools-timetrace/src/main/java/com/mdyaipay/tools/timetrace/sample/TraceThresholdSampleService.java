package com.mdyaipay.tools.timetrace.sample;

import com.mdyaipay.tools.timetrace.TimeTrace;

/**
 * 单测用：验证 {@link TimeTrace#reportThresholdMillis()} 低于门槛时不输出报告。
 */
public class TraceThresholdSampleService {

    /**
     * 总耗时约 1ms，低于 50ms 门槛，不应触发 {@link com.mdyaipay.tools.timetrace.TimeTraceListener}。
     */
    @TimeTrace(value = "below-threshold", reportThresholdMillis = 50)
    public void fastCall() {
        busyWork(1);
    }

    private static void busyWork(int millis) {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            /* 刻意空循环 */
        }
    }
}
