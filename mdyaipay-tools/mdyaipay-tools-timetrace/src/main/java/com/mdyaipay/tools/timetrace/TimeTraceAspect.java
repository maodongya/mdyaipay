package com.mdyaipay.tools.timetrace;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Spring AOP 切面：{@link TimeTrace} 入口报告 + 会话内 Spring Bean 调用树。
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

    /**
     * 带 Spring .stereotype 的 Bean 的 public 方法（会话内计帧；不含 timetrace 自身包）。
     */
    @Pointcut(
            "execution(public * *(..)) && ("
                    + "@within(org.springframework.stereotype.Service) || "
                    + "@within(org.springframework.stereotype.Repository) || "
                    + "@within(org.springframework.stereotype.Component) || "
                    + "@within(org.springframework.stereotype.Controller)"
                    + ") && !within(com.mdyaipay.tools.timetrace.autoconfigure..*)"
                    + " && !within(com.mdyaipay.tools.timetrace.TimeTraceAspect)")
    void tracedSpringBeanMethod() {
    }

    @Around("traceEntry()")
    public Object aroundEntry(ProceedingJoinPoint joinPoint) throws Throwable {
        return TimeTraceEntrySupport.aroundEntry(joinPoint, TimeTraceSupport.globalListener());
    }

    @Around("tracedSpringBeanMethod() && !traceEntry()")
    public Object aroundNestedSpringCall(ProceedingJoinPoint joinPoint) throws Throwable {
        return TimeTraceNestedSupport.aroundNested(joinPoint);
    }
}
