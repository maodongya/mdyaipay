/**
 * Spring AOP 方法耗时追踪（{@code mdyaipay-tools-timetrace}）。
 * <p>
 * 在 Spring Bean 的方法或类上使用 {@link com.mdyaipay.tools.timetrace.TimeTrace}；
 * Spring Boot 下引入 {@code spring-boot-starter-aop} 后由
 * {@link com.mdyaipay.tools.timetrace.autoconfigure.TimeTraceAutoConfiguration} 自动注册切面。
 * <p>
 * 设计说明见 {@code docs/timetrace-design.md}。
 */
package com.mdyaipay.tools.timetrace;
