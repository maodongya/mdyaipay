package com.mdyaipay.tools.timetrace.sample;

import org.springframework.stereotype.Service;

/** 单测用：演示入口 {@link TimeTrace} 会话内跨 Bean 调用出现在报告树中。 */
@Service
public class TraceCollaboratorService {

    public int bump(int value) {
        busyWork(2);
        return value + 10;
    }

    private static void busyWork(int millis) {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            /* 刻意空循环 */
        }
    }
}
