package com.mdyaipay.tools.ratelimit.redis;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.redis.script.RateLimitScripts;
import io.lettuce.core.api.StatefulRedisConnection;

import java.time.Duration;
import java.util.Objects;

/**
 * Redis 限流门面：Java 预计算时间与 key，Lua 只做原子读写。
 * <p>
 * <b>不负责</b> 创建 Redis 客户端——由调用方注入 {@link StatefulRedisConnection}。
 */
public final class RedisRateLimiter implements RateLimiter, AutoCloseable {

    private final StatefulRedisConnection<String, String> connection;
    private final boolean closeConnection;
    private final RedisScriptSupport scripts;

    /**
     * @param connection      Redis 连接，非 null
     * @param closeConnection {@code true} 时 {@link #close()} 关闭连接
     */
    public RedisRateLimiter(StatefulRedisConnection<String, String> connection, boolean closeConnection) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.closeConnection = closeConnection;
        this.scripts = new RedisScriptSupport(connection);
    }

    /**
     * 包装已有连接：{@link #close()} 不关闭连接。
     *
     * @param connection Redis 连接
     * @return 限流器
     */
    public static RedisRateLimiter wrap(StatefulRedisConnection<String, String> connection) {
        return new RedisRateLimiter(connection, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        validate(key, policy);
        RateLimitScripts script = RateLimitScripts.forAlgorithm(policy.algorithm());
        String redisKey = RedisKeyNames.of(policy.algorithm(), key);
        RedisServerTime time = scripts.serverTime();
        return switch (policy.algorithm()) {
            case FIXED_WINDOW -> toDecision(
                    scripts.eval(
                            script,
                            fixedWindowBucketKey(redisKey, time, policy.window().toMillis()),
                            Long.toString(policy.limit()),
                            Long.toString(policy.window().toMillis())),
                    policy.algorithm());
            case SLIDING_WINDOW_COUNTER -> toDecision(
                    scripts.eval(
                            script,
                            redisKey,
                            slidingCounterArgv(policy, time)),
                    policy.algorithm());
            case SLIDING_WINDOW_LOG -> slidingWindowLogDecision(
                    scripts.eval(
                            script,
                            redisKey,
                            Long.toString(policy.limit()),
                            Long.toString(time.epochMillis()),
                            Long.toString(time.slidingWindowStart(policy.window().toMillis())),
                            time.zsetMember(),
                            Long.toString(policy.window().toMillis())),
                    policy,
                    time);
            case TOKEN_BUCKET -> toDecision(
                    scripts.eval(
                            script,
                            redisKey,
                            Long.toString(policy.limit()),
                            Double.toString(policy.refillRatePerSecond()),
                            Long.toString(time.epochMillis()),
                            Long.toString(RedisServerTime.tokenBucketKeyTtlMs(
                                    policy.limit(), policy.refillRatePerSecond()))),
                    policy.algorithm());
        };
    }

    /**
     * 固定窗口 bucket key：baseKey + 对齐 windowStart。
     */
    private static String fixedWindowBucketKey(String baseKey, RedisServerTime time, long windowMs) {
        return baseKey + ':' + time.fixedWindowStart(windowMs);
    }

    /**
     * 滑动窗口计数 Lua ARGV：limit、段序号、段内 elapsed、segmentMs、PEXPIRE。
     */
    private static String[] slidingCounterArgv(RateLimitPolicy policy, RedisServerTime time) {
        long windowMs = policy.window().toMillis();
        int segments = policy.slidingSegments();
        long segmentMs = Math.max(1L, windowMs / segments);
        return new String[] {
            Long.toString(policy.limit()),
            Long.toString(time.slidingSegmentIndex(segmentMs)),
            Long.toString(time.elapsedInSegment(segmentMs)),
            Long.toString(segmentMs),
            Long.toString(windowMs * 2L)
        };
    }

    /**
     * 滑动窗口日志：拒绝时 Lua 第 4 字段为最旧 score，retryAfter 在 Java 侧计算。
     */
    private static RateLimitDecision slidingWindowLogDecision(
            RedisLuaResult result, RateLimitPolicy policy, RedisServerTime time) {
        if (result.allowed()) {
            return new RateLimitDecision(
                    true,
                    result.remaining(),
                    result.limit(),
                    Duration.ZERO,
                    RateLimitAlgorithm.SLIDING_WINDOW_LOG);
        }
        long windowMs = policy.window().toMillis();
        long retryMs = time.retryAfterMsFromOldest(result.retryAfterMs(), windowMs);
        return new RateLimitDecision(
                false,
                0,
                result.limit(),
                Duration.ofMillis(retryMs),
                RateLimitAlgorithm.SLIDING_WINDOW_LOG);
    }

    /**
     * 将 Lua 结果转为契约 Decision（第 4 字段即 retryAfterMs）。
     */
    private static RateLimitDecision toDecision(RedisLuaResult result, RateLimitAlgorithm algorithm) {
        Duration retryAfter = result.retryAfterMs() <= 0
                ? Duration.ZERO
                : Duration.ofMillis(result.retryAfterMs());
        return new RateLimitDecision(
                result.allowed(),
                result.remaining(),
                result.limit(),
                retryAfter,
                algorithm);
    }

    /**
     * 校验 key 与 policy。
     */
    private static void validate(String key, RateLimitPolicy policy) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
    }

    /**
     * 按构造参数决定是否关闭底层连接。
     */
    @Override
    public void close() {
        if (closeConnection) {
            connection.close();
        }
    }
}
