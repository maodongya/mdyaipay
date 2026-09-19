/**
 * 限流核心：{@code RateLimiter} 契约、{@code RateLimitPolicy} 策略模型、内存算法与键解析 SPI。
 * <p>
 * <b>不负责</b> Redis 连接与 Lua——见 {@code mdyaipay-tools-ratelimit-redis}；
 * <b>不负责</b> Spring/Gateway 过滤器——见 {@code mdyaipay-tools-ratelimit-spring-boot}。
 * 设计详见 {@code docs/superpowers/specs/2026-09-20-ratelimit-design.md}。
 */
package com.mdyaipay.tools.ratelimit;
