package com.mdyaipay.tools.trace.slice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要在单服务内参与 Trace 子 span 切片的 Spring Bean 方法或类。
 * <p>
 * 与 {@link TraceSliceAspect} 配合：在已有 {@link com.mdyaipay.tools.trace.TraceContext} 时，
 * 每次进入被匹配的方法会 {@link com.mdyaipay.tools.trace.TraceSnapshot#childSpan()} 并刷新 MDC。
 * 无上下文时不改变线程状态（例如未经过 HTTP/Dubbo 入口 Filter）。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TraceSlice {

    /**
     * 预留展示名；当前切片仅切换 spanId，不参与日志格式化。
     */
    String value() default "";
}
