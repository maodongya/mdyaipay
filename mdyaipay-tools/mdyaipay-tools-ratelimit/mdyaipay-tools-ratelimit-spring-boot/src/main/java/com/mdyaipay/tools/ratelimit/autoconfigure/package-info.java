/**
 * 限流 Spring Boot 自动配置：{@code mdyaipay.ratelimit.*} 属性、Gateway GlobalFilter 与可选 Servlet Filter。
 * <p>
 * 未配置 Redis 时可回退 core 内存限流（仅单机有效）。Lettuce 后端依赖 {@code ratelimit-redis}；Redisson 后端依赖 {@code ratelimit-redisson}（optional）。
 */
package com.mdyaipay.tools.ratelimit.autoconfigure;
