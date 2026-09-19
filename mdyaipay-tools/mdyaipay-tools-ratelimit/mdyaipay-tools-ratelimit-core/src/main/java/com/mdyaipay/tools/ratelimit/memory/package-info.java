/**
 * 内存限流算法实现：固定窗口、滑动窗口计数、令牌桶及门面。
 * <p>
 * <b>不负责</b> 跨 JVM 一致性——仅单机与单测；分布式见 {@code mdyaipay-tools-ratelimit-redis}。
 */
package com.mdyaipay.tools.ratelimit.memory;
