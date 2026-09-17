package com.mdyaipay.tools.timetrace;

import org.aspectj.lang.ProceedingJoinPoint;

/** 入口 {@link TimeTrace} 会话内，对 Spring Bean 方法调用计帧（不新开报告会话）。 */
public final class TimeTraceNestedSupport {

    private TimeTraceNestedSupport() {
    }

    public static Object aroundNested(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!TimeTraceContext.isTracing()) {
            return joinPoint.proceed();
        }
        String signature = joinPoint.getSignature().toShortString();
        TimeTraceNode node = TimeTraceContext.enterFrame(signature);
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            TimeTraceContext.leaveFrame(node, System.nanoTime() - start);
        }
    }
}
