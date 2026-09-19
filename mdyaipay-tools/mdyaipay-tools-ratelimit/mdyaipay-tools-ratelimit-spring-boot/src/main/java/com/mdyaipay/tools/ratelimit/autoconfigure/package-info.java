/**
 * 限流 Spring Boot 自动配置：{@code mdyaipay.ratelimit.*} 属性、Gateway GlobalFilter 与可选 Servlet Filter。
 * <p>
 * 未配置 Redis 时可回退 core 内存限流（仅单机有效）。Redis 后端依赖 {@code mdyaipay-tools-ratelimit-redis}（optional）。
 */
package com.mdyaipay.tools.ratelimit.autoconfigure;
