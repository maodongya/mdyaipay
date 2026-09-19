/**
 * Redis 限流实现：Lettuce 客户端与各算法 Lua 脚本（固定窗口、滑动窗口计数、令牌桶）。
 * <p>
 * <b>不负责</b> 限流契约定义与内存 fallback——见 {@code mdyaipay-tools-ratelimit-core}。
 */
package com.mdyaipay.tools.ratelimit.redis;
