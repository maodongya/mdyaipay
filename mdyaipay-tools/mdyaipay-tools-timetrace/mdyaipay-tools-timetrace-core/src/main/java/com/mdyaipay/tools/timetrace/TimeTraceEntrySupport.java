// 本类所在包：timetrace 核心工具
package com.mdyaipay.tools.timetrace;

// AspectJ 环绕通知连接点，用于 proceed 与读取签名
import org.aspectj.lang.ProceedingJoinPoint;
// 方法签名类型，便于反射读取 Method 与注解
import org.aspectj.lang.reflect.MethodSignature;

// JDK 反射 Method，读取方法级 @TimeTrace
import java.lang.reflect.Method;

/**
 * {@link TimeTraceAspect} 入口 advice 的核心逻辑。
 * <p>
 * 负责开启/结束一次根级计时会话、解析注解配置，并在结束时按阈值决定是否回调 {@link TimeTraceListener}。
 * 不负责会话内的嵌套 Spring Bean 计帧（见 {@link TimeTraceNestedSupport}）。
 */
public final class TimeTraceEntrySupport {

    /**
     * 工具类禁止实例化。
     */
    private TimeTraceEntrySupport() {
    }

    /**
     * 处理带 {@link TimeTrace} 的入口切点：若已在追踪中则委托嵌套逻辑，否则开启根会话并产出报告。
     *
     * @param joinPoint 当前环绕通知连接点
     * @param listener  根会话结束时的报告回调（可为全局监听器）
     * @return 被拦截方法的返回值
     * @throws Throwable 被拦截方法抛出的任意异常原样向上传播
     */
    public static Object aroundEntry(ProceedingJoinPoint joinPoint, TimeTraceListener listener) throws Throwable {
        /* 功能块：已在追踪 — 仅计帧，不新开根报告 */
        // 若当前线程已有 timetrace 根会话，走嵌套路径
        if (TimeTraceContext.isTracing()) {
            // 嵌套调用：enter/leave 帧，不触发 onComplete
            return TimeTraceNestedSupport.aroundNested(joinPoint);
        }

        /* 功能块：解析注解与报告策略 — 决定 label、是否报告、耗时阈值 */
        // 从方法或类型上解析 @TimeTrace（可能为 null，如仅类级切点但未标注）
        TimeTrace annotation = resolveAnnotation(joinPoint);
        // 报告展示用标签：注解 value 优先，否则用方法短签名
        String label = buildLabel(joinPoint, annotation);
        // 无注解时默认完成即报告；有注解时读 reportOnComplete
        boolean report = annotation == null || annotation.reportOnComplete();
        // 报告耗时阈值（毫秒）；无注解时为 0 表示不按阈值过滤
        long thresholdMillis = annotation != null ? annotation.reportThresholdMillis() : 0L;
        // 在线程上下文中开启根会话，并绑定 listener 与报告开关
        TimeTraceContext.Session session = TimeTraceContext.begin(label, listener, report, thresholdMillis);

        /* 功能块：根帧计时 — 记录入口方法自身耗时并挂到调用树 */
        // 根节点在树中的展示名（方法短签名）
        String signature = joinPoint.getSignature().toShortString();
        // 压栈根帧，返回对应树节点
        TimeTraceNode root = TimeTraceContext.enterFrame(signature);
        // 根方法执行起点（纳秒，用于 finally 算 elapsed）
        long start = System.nanoTime();
        // 若 proceed 抛错，保留引用供报告与 shouldEmitReport 使用
        Throwable error = null;
        try {
            // 执行真实业务方法
            return joinPoint.proceed();
        } catch (Throwable t) {
            // 记录异常，不改变向上抛出的语义
            error = t;
            throw t;
        } finally {
            /* 功能块：收尾 — 弹栈、组报告、按条件回调、释放上下文 */
            // 根方法 wall-clock 耗时（纳秒）
            long elapsed = System.nanoTime() - start;
            // 将根帧耗时写入调用树并出栈
            TimeTraceContext.leaveFrame(root, elapsed);
            // 组装完整报告载荷（含子树与是否出错）
            TimeTraceReport reportPayload = new TimeTraceReport(
                    session.label,
                    session.root,
                    elapsed,
                    error);
            // 根据开关、异常、阈值决定是否通知监听器
            if (shouldEmitReport(session, elapsed, error)) {
                session.listener.onComplete(reportPayload);
            }
            // 清除线程上的 timetrace 会话，避免泄漏到线程池
            TimeTraceContext.end();
        }
    }

    /**
     * 判断根会话结束时是否应触发 {@link TimeTraceListener#onComplete(TimeTraceReport)}。
     *
     * @param session       当前根会话（含 reportOnComplete 与阈值配置）
     * @param elapsedNanos  根方法耗时（纳秒）
     * @param error         proceed 过程中捕获的异常，无则为 null
     * @return 为 true 时应对外发送报告
     */
    private static boolean shouldEmitReport(TimeTraceContext.Session session, long elapsedNanos, Throwable error) {
        // 注解或 begin 时关闭了完成报告
        if (!session.reportOnComplete) {
            return false;
        }
        // 出错时始终报告，便于排查
        if (error != null) {
            return true;
        }
        // 阈值为 0 或未配置：任意耗时都报告
        if (session.reportThresholdMillis <= 0) {
            return true;
        }
        // 将毫秒阈值换算为纳秒，与 elapsedNanos 同单位比较
        long thresholdNanos = session.reportThresholdMillis * 1_000_000L;
        // 仅当耗时达到或超过阈值才报告（慢请求过滤）
        return elapsedNanos >= thresholdNanos;
    }

    /**
     * 从连接点解析 {@link TimeTrace}：方法注解优先，其次声明类或目标类上的类型注解。
     *
     * @param joinPoint 入口切点
     * @return 解析到的注解实例；无法解析时返回 null
     */
    private static TimeTrace resolveAnnotation(ProceedingJoinPoint joinPoint) {
        // 标准 Java 方法切点：可读 Method 与 declaring class
        if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
            // 被拦截的 Method 对象
            Method method = methodSignature.getMethod();
            // 方法上的 @TimeTrace 优先级最高
            TimeTrace onMethod = method.getAnnotation(TimeTrace.class);
            if (onMethod != null) {
                return onMethod;
            }
            // 代理场景用 target 类，否则用接口/声明类
            Class<?> targetClass = joinPoint.getTarget() != null
                    ? joinPoint.getTarget().getClass()
                    : method.getDeclaringClass();
            // 类型级注解作为兜底
            return targetClass.getAnnotation(TimeTrace.class);
        }
        // 非 MethodSignature（少见）：仅尝试 target 类上的类型注解
        Object target = joinPoint.getTarget();
        if (target != null) {
            return target.getClass().getAnnotation(TimeTrace.class);
        }
        // 无 target 且非方法签名，无法解析
        return null;
    }

    /**
     * 构造报告与上下文使用的展示标签。
     *
     * @param joinPoint  入口切点
     * @param annotation 已解析的注解，可为 null
     * @return 非空标签字符串
     */
    private static String buildLabel(ProceedingJoinPoint joinPoint, TimeTrace annotation) {
        // 注解显式指定 value 时使用自定义标签
        if (annotation != null && !annotation.value().isBlank()) {
            return annotation.value();
        }
        // 默认与方法短签名一致，便于日志对齐
        return joinPoint.getSignature().toShortString();
    }
}
