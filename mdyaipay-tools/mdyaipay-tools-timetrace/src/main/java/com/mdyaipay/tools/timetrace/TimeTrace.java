package com.mdyaipay.tools.timetrace;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要追踪执行时间的 Spring Bean 方法或类。
 * <p>
 * 配合 {@link TimeTraceAspect} 与 {@code spring-boot-starter-aop} 使用；
 * 仅对容器管理的 Bean 的 public 方法生效（同类内部自调用不会被再次拦截）。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TimeTrace {

    /**
     * 报告中的展示名称；为空时使用连接点短签名（如 {@code run(..)}）。
     */
    String value() default "";

    /**
     * 入口方法结束（正常返回或抛错）后，是否允许回调 {@link TimeTraceListener}。
     */
    boolean reportOnComplete() default true;

    /**
     * 仅当总耗时 ≥ 该阈值（毫秒）时输出报告；{@code 0} 表示不设门槛。异常结束始终输出。
     */
    long reportThresholdMillis() default 0;
}
