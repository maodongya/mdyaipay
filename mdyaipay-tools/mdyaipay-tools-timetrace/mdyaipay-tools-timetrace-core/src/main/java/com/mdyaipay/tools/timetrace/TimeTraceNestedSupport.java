// 本类所在包：timetrace 核心工具
package com.mdyaipay.tools.timetrace;

// AspectJ 环绕通知连接点，用于 proceed 与读取方法签名
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 入口 {@link TimeTrace} 会话内，对 Spring Bean 等方法调用计帧（不新开报告会话）。
 * <p>
 * 仅在 {@link TimeTraceContext} 已有根会话时压栈/出栈子帧；无会话时直接 proceed，避免误计帧。
 * 不负责开启根会话、组装 {@link TimeTraceReport} 或回调 {@link TimeTraceListener}（见 {@link TimeTraceEntrySupport}）。
 */
public final class TimeTraceNestedSupport {

    /**
     * 工具类禁止实例化。
     */
    private TimeTraceNestedSupport() {
    }

    /**
     * 嵌套切点环绕逻辑：有活跃追踪则计一帧并挂到当前调用树，否则透明转发。
     *
     * @param joinPoint 嵌套 Spring Bean 等切点的连接点
     * @return 被拦截方法的返回值
     * @throws Throwable 被拦截方法抛出的任意异常原样向上传播
     */
    public static Object aroundNested(ProceedingJoinPoint joinPoint) throws Throwable {
        // 当前线程未在 timetrace 根会话中：不参与调用树，直接执行
        if (!TimeTraceContext.isTracing()) {
            return joinPoint.proceed();
        }
        // 子帧在报告树中的展示名（方法短签名）
        String signature = joinPoint.getSignature().toShortString();
        // 压栈子帧，返回对应树节点供 leave 时使用
        TimeTraceNode node = TimeTraceContext.enterFrame(signature);
        // 本子帧执行起点（纳秒）
        long start = System.nanoTime();
        try {
            // 执行真实业务方法
            return joinPoint.proceed();
        } finally {
            // 无论成功或异常，都将耗时写入树节点并出栈
            TimeTraceContext.leaveFrame(node, System.nanoTime() - start);
        }
    }
}
