package com.mdyaipay.tools.ratelimit.redisson;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 经 Redisson 读取 Redis {@code TIME}，供滑动窗口日志参数预计算。
 * <p>
 * <b>不负责</b> 限流判定——仅时间戳与 ZSET member 唯一后缀。
 */
public final class RedissonServerTime {

    private static final String TIME_SCRIPT = "return redis.call('TIME')";

    private static final AtomicLong LOCAL_SEQ = new AtomicLong();

    private final long epochMillis;
    private final long microPart;

    private RedissonServerTime(long epochMillis, long microPart) {
        this.epochMillis = epochMillis;
        this.microPart = microPart;
    }

    /**
     * 读取 Redis 服务器当前时刻。
     *
     * @param redisson Redisson 客户端，非 null
     * @return 时刻快照
     */
    @SuppressWarnings("unchecked")
    public static RedissonServerTime read(RedissonClient redisson) {
        Objects.requireNonNull(redisson, "redisson");
        RScript script = redisson.getScript(StringCodec.INSTANCE);
        List<Object> parts = script.eval(RScript.Mode.READ_ONLY, TIME_SCRIPT, RScript.ReturnType.MULTI);
        long seconds = toLong(parts.get(0));
        long micro = toLong(parts.get(1));
        long epochMillis = seconds * 1000L + micro / 1000L;
        return new RedissonServerTime(epochMillis, micro);
    }

    /**
     * @return 毫秒时间戳（与 ZSET score 对齐）
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
     * 将 Redis TIME 片段转为 long。
     */
    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("unsupported TIME part: " + value);
    }
}
