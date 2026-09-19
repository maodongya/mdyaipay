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
 * Redis 限流门面：按 {@link RateLimitPolicy#algorithm()} 执行对应 Lua 脚本。
 * <p>
 * <b>不负责</b> 创建 Redis 客户端——由调用方注入 {@link StatefulRedisConnection}；
 * 本类在 {@code closeConnection=true} 时关闭连接。
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
        RedisLuaResult result = switch (policy.algorithm()) {
            case FIXED_WINDOW -> scripts.eval(
                    script,
                    redisKey,
                    Long.toString(policy.limit()),
                    Long.toString(policy.window().toMillis()));
            case SLIDING_WINDOW_COUNTER -> scripts.eval(
                    script,
                    redisKey,
                    Long.toString(policy.limit()),
                    Long.toString(policy.window().toMillis()),
                    Integer.toString(policy.slidingSegments()));
            case TOKEN_BUCKET -> scripts.eval(
                    script,
                    redisKey,
                    Long.toString(policy.limit()),
                    Double.toString(policy.refillRatePerSecond()));
        };
        return toDecision(result, policy.algorithm());
    }

    /**
     * 将 Lua 结果转为契约 Decision。
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
