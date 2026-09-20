package com.mdyaipay.tools.ratelimit.redis;

import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 从 Redis {@code TIME} 读取服务器时钟，供 Lua 脚本参数预计算。
 * <p>
 * <b>不负责</b> 限流判定——仅时间戳与 ZSET member 唯一后缀。
 */
public final class RedisServerTime {

    private static final AtomicLong LOCAL_SEQ = new AtomicLong();

    private final long epochMillis;
    private final long microPart;

    private RedisServerTime(long epochMillis, long microPart) {
        this.epochMillis = epochMillis;
        this.microPart = microPart;
    }

    /**
     * 读取 Redis 服务器当前时刻。
     *
     * @param sync 同步命令接口，非 null
     * @return 时刻快照
     */
    public static RedisServerTime read(RedisCommands<String, String> sync) {
        Objects.requireNonNull(sync, "sync");
        List<String> parts = sync.time();
        long seconds = Long.parseLong(parts.get(0));
        long micro = Long.parseLong(parts.get(1));
        long epochMillis = seconds * 1000L + micro / 1000L;
        return new RedisServerTime(epochMillis, micro);
    }

    /**
     * @return 毫秒时间戳（与 Lua 侧 ZSET score 对齐）
     */
    public long epochMillis() {
        return epochMillis;
    }

    /**
     * 生成 ZSET member：Redis 微秒 + 本地序号，避免同毫秒并发碰撞。
     *
     * @return member 字符串
     */
    public String zsetMember() {
        return epochMillis + ":" + microPart + ":" + LOCAL_SEQ.incrementAndGet();
    }

    /**
     * 计算滑动窗口左边界（含）：{@code now - windowMs}。
     *
     * @param windowMs 窗口长度毫秒
     * @return windowStart
     */
    public long slidingWindowStart(long windowMs) {
        return epochMillis - windowMs;
    }

    /**
     * 固定窗口对齐起点。
     *
     * @param windowMs 窗口长度毫秒
     * @return 对齐后的 windowStart
     */
    public long fixedWindowStart(long windowMs) {
        return (epochMillis / windowMs) * windowMs;
    }

    /**
     * 拒绝时由最旧 score 与当前时刻推算 retryAfter。
     *
     * @param oldestScoreMs 窗口内最旧请求 score
     * @param windowMs      窗口长度毫秒
     * @return 建议重试毫秒，至少 1
     */
    public long retryAfterMsFromOldest(long oldestScoreMs, long windowMs) {
        return Math.max(1L, oldestScoreMs + windowMs - epochMillis);
    }

    /**
     * 滑动窗口计数：当前段序号（从 epoch 起按 segmentMs 划分）。
     *
     * @param segmentMs 单段长度毫秒，至少 1
     * @return 段序号
     */
    public long slidingSegmentIndex(long segmentMs) {
        long segMs = Math.max(1L, segmentMs);
        return epochMillis / segMs;
    }

    /**
     * 滑动窗口计数：当前段内已过去毫秒。
     *
     * @param segmentMs 单段长度毫秒，至少 1
     * @return elapsed
     */
    public long elapsedInSegment(long segmentMs) {
        long segMs = Math.max(1L, segmentMs);
        return epochMillis % segMs;
    }

    /**
     * 令牌桶 hash 的 PEXPIRE：与原先 Lua 内公式一致，由 Java 预计算。
     *
     * @param capacity 桶容量
     * @param refillRatePerSecond 每秒补充令牌
     * @return TTL 毫秒，至少 1000
     */
    public static long tokenBucketKeyTtlMs(long capacity, double refillRatePerSecond) {
        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("refillRatePerSecond must be positive");
        }
        return Math.max(1000L, (long) Math.ceil(capacity / refillRatePerSecond * 2000.0));
    }
}
