package com.mdyaipay.tools.timetrace.sample;

import com.mdyaipay.tools.timetrace.TimeTrace;

/**
 * 单测用示例：演示 {@link TimeTrace} 入口耗时（Spring Bean 代理）。
 * <p>
 * 不参与业务模块依赖，仅位于 {@code sample} 包。
 */
public class TraceSampleService {

    private final TraceCollaboratorService collaborator;

    public TraceSampleService(TraceCollaboratorService collaborator) {
        this.collaborator = collaborator;
    }

    /**
     * 入口方法：串联两段子调用并返回合并结果，总耗时约数毫秒（空转模拟）。
     */
    @TimeTrace("sample-flow")
    public int run(int seed) {
        /* 功能块：阶段 A — 模拟第一段业务耗时 */
        int a = stepA(seed);

        /* 功能块：跨 Bean 调用 — 应出现在 TimeTrace 报告树 */
        int fromCollaborator = collaborator.bump(a);

        /* 功能块：阶段 B — 依赖 A 的结果继续处理 */
        int b = stepB(fromCollaborator);

        /* 功能块：合并结果 — 供断言与报告校验 */
        return a + b;
    }

    /** 第一段处理：递增并空转等待。 */
    private int stepA(int value) {
        busyWork(2);
        return value + 1;
    }

    /** 第二段处理：倍增并空转等待。 */
    private int stepB(int value) {
        busyWork(3);
        return value * 2;
    }

    /** 空转等待指定毫秒，单测中避免 {@code Thread.sleep} 带来的 CI 抖动。 */
    private static void busyWork(int millis) {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            /* 刻意空循环 */
        }
    }
}
