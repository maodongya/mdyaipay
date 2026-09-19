package com.mdyaipay.tools.trace.slice;

import com.mdyaipay.tools.trace.autoconfigure.TraceProperties;
import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Spring AOP 切面：单服务内按「切片」为每层 Bean 调用切换子 span。
 * <p>
 * 默认匹配常见 Spring .stereotype Bean 的 public 方法；亦可通过 {@link TraceSlice} 自定义范围。
 * <b>不负责</b>跨进程传播或 Span 上报（见 trace-core / SkyWalking Agent）。
 */
@Aspect
public class TraceSliceAspect {

    private final TraceMdcSupport mdcSupport;
    private final TraceProperties.Slice sliceProperties;

    /**
     * @param mdcSupport       MDC 写入；非 null
     * @param sliceProperties  切片开关与范围配置；非 null
     */
    public TraceSliceAspect(TraceMdcSupport mdcSupport, TraceProperties properties) {
        this.mdcSupport = mdcSupport;
        this.sliceProperties = properties.getSlice();
    }

    /** 匹配方法或类上显式标注 {@link TraceSlice} 的 Spring 管理 Bean。 */
    @Pointcut("@annotation(com.mdyaipay.tools.trace.slice.TraceSlice)")
    void methodMarked() {
    }

    @Pointcut("@within(com.mdyaipay.tools.trace.slice.TraceSlice)")
    void typeMarked() {
    }

    @Pointcut("methodMarked() || typeMarked()")
    void sliceMarked() {
    }

    /**
     * 匹配带 Spring .stereotype 的 Bean public 方法，用于请求内默认嵌套切片；
     * 排除 trace 工具包自身，避免 Filter/切面递归改 span。
     */
    @Pointcut(
            "execution(public * *(..)) && ("
                    + "@within(org.apache.ibatis.annotations.Mapper) || "
                    + "@within(org.springframework.stereotype.Service) || "
                    + "@within(org.springframework.stereotype.Repository) || "
                    + "@within(org.springframework.stereotype.Component) || "
                    + "@within(org.springframework.stereotype.Controller) ||"
                    + "@within(org.springframework.web.bind.annotation.RestController)"
                    + ") && !within(com.mdyaipay.tools.trace.autoconfigure..*)"
                    + " && !within(com.mdyaipay.tools.trace.http..*)"
                    + " && !within(com.mdyaipay.tools.trace.dubbo..*)"
                    + " && !within(com.mdyaipay.tools.trace.mdc..*)")
    void tracedSpringBeanMethod() {
    }

    @Around("sliceMarked()")
    public Object aroundExplicitSlice(ProceedingJoinPoint joinPoint) throws Throwable {
        return TraceSliceSupport.aroundWithChildSpan(joinPoint, mdcSupport, sliceProperties.isLogOnComplete());
    }

    @Around("tracedSpringBeanMethod() && !sliceMarked()")
    public Object aroundNestedSpringBean(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!sliceProperties.isNestedSpringBeans()) {
            return joinPoint.proceed();
        }
        return TraceSliceSupport.aroundWithChildSpan(joinPoint, mdcSupport, sliceProperties.isLogOnComplete());
    }
}
