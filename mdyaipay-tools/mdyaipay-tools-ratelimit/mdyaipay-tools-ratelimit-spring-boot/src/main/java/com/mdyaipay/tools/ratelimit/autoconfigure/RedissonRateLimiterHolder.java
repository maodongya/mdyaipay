package com.mdyaipay.tools.ratelimit.autoconfigure;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import com.mdyaipay.tools.ratelimit.RateLimitDecision;
import com.mdyaipay.tools.ratelimit.RateLimitPolicy;
import com.mdyaipay.tools.ratelimit.RateLimiter;
import com.mdyaipay.tools.ratelimit.redisson.RedissonSlidingWindowLogRateLimiter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.util.Objects;

/**
 * 持有 {@link RedissonClient} 的 {@link RateLimiter} 包装：仅支持滑动窗口日志，销毁时 shutdown 客户端。
 */
public final class RedissonRateLimiterHolder implements RateLimiter, DisposableBean {

    private final RedissonClient client;
    private final RedissonSlidingWindowLogRateLimiter delegate;

    /**
     * @param uri     Redis URI（{@code redis://host:port}），非 blank
     * @param timeout 命令超时，可为 null
     */
    public RedissonRateLimiterHolder(String uri, Duration timeout) {
        Objects.requireNonNull(uri, "uri");
        Config config = new Config();
        var single = config.useSingleServer().setAddress(uri);
        if (timeout != null) {
            single.setTimeout((int) timeout.toMillis());
        }
        this.client = Redisson.create(config);
        this.delegate = RedissonSlidingWindowLogRateLimiter.wrap(client);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 前置条件：{@code policy.algorithm()} 必须为 {@link RateLimitAlgorithm#SLIDING_WINDOW_LOG}。
     */
    @Override
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        if (policy.algorithm() != RateLimitAlgorithm.SLIDING_WINDOW_LOG) {
            throw new IllegalArgumentException(
                    "redisson backend only supports SLIDING_WINDOW_LOG, got " + policy.algorithm());
        }
        return delegate.tryAcquire(key, policy);
    }

    /**
     * 关闭 Redisson 客户端。
     */
    @Override
    public void destroy() {
        client.shutdown();
    }
}
