package com.mdyaipay.tools.ratelimit.redis.script;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 限流 Lua 脚本枚举：绑定算法与 classpath 资源路径。
 */
public enum RateLimitScripts {

    /** 固定窗口。 */
    FIXED_WINDOW(RateLimitAlgorithm.FIXED_WINDOW, "ratelimit/lua/fixed_window.lua"),

    /** 滑动窗口计数。 */
    SLIDING_WINDOW_COUNTER(RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, "ratelimit/lua/sliding_window_counter.lua"),

    /** 令牌桶。 */
    TOKEN_BUCKET(RateLimitAlgorithm.TOKEN_BUCKET, "ratelimit/lua/token_bucket.lua");

    private final RateLimitAlgorithm algorithm;
    private final String resourcePath;

    RateLimitScripts(RateLimitAlgorithm algorithm, String resourcePath) {
        this.algorithm = algorithm;
        this.resourcePath = resourcePath;
    }

    /**
     * @return 对应算法
     */
    public RateLimitAlgorithm algorithm() {
        return algorithm;
    }

    /**
     * @return classpath 资源路径
     */
    public String resourcePath() {
        return resourcePath;
    }

    /**
     * 按算法查找脚本枚举。
     *
     * @param algorithm 算法，非 null
     * @return 脚本枚举
     */
    public static RateLimitScripts forAlgorithm(RateLimitAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm");
        for (RateLimitScripts script : values()) {
            if (script.algorithm == algorithm) {
                return script;
            }
        }
        throw new IllegalArgumentException("unsupported algorithm: " + algorithm);
    }

    /**
     * 从 classpath 读取脚本正文。
     * <p>幂等：是（每次读资源；可被调用方缓存）。
     *
     * @return Lua 源码
     */
    public String loadBody() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = RateLimitScripts.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("missing Lua resource: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to load " + resourcePath, e);
        }
    }
}
