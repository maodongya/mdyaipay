package com.mdyaipay.tools.timetrace;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Spring AOP 切面：拦截带 {@link TimeTrace} 的 Spring Bean 方法，统计入口耗时并输出报告。
 * <p>
 * Spring Boot 应用引入 {@code spring-boot-starter-aop} 与 {@code mdyaipay-tools-timetrace} 即可
 * （{@link com.mdyaipay.tools.timetrace.autoconfigure.TimeTraceAutoConfiguration} 自动注册本类）；
 * 非 Boot 场景需自行注册 Bean 并启用 {@link org.springframework.context.annotation.EnableAspectJAutoProxy}。
 */
@Aspect
public class TimeTraceAspect {

    @Pointcut("@annotation(com.mdyaipay.tools.timetrace.TimeTrace)")
    void methodMarked() {
    }

    @Pointcut("@within(com.mdyaipay.tools.timetrace.TimeTrace)")
    void typeMarked() {
    }

    @Pointcut("methodMarked() || typeMarked()")
    void traceEntry() {
    }

    @Around("traceEntry()")
    public Object aroundEntry(ProceedingJoinPoint joinPoint) throws Throwable {
        return TimeTraceEntrySupport.aroundEntry(joinPoint, TimeTraceSupport.globalListener());
    }
}
