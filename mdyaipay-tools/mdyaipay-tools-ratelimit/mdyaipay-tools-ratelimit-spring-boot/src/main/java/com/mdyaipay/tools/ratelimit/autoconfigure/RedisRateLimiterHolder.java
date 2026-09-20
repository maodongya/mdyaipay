package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.redis.RedisRateLimiter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.util.Objects;

/**
 * 持有 Redis 客户端与连接的 {@link RateLimiter} 包装，销毁时释放资源。
 */
public final class RedisRateLimiterHolder implements RateLimiter, DisposableBean {

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisRateLimiter delegate;

    /**
     * @param uri     Redis URI，非 blank
     * @param timeout 命令超时，可为 null
     */
    public RedisRateLimiterHolder(String uri, Duration timeout) {
        Objects.requireNonNull(uri, "uri");
        RedisURI redisURI = RedisURI.create(uri);
        if (timeout != null) {
            redisURI.setTimeout(timeout);
        }
        this.client = RedisClient.create(redisURI);
        this.connection = client.connect();
        this.delegate = RedisRateLimiter.wrap(connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        return delegate.tryAcquire(key, policy);
    }

    /**
     * 关闭连接与客户端。
     */
    @Override
    public void destroy() {
        connection.close();
        client.shutdown();
    }
}
