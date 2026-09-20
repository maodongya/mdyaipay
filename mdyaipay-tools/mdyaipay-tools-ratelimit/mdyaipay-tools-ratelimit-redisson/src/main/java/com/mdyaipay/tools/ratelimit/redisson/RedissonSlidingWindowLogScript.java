package com.mdyaipay.tools.ratelimit.redisson;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 滑动窗口日志 Lua：经 {@link RScript} 装载与 {@code EVALSHA} 执行。
 * <p>
 * <b>不负责</b> 连接生命周期——由调用方持有 {@link RedissonClient}。
 */
public final class RedissonSlidingWindowLogScript {

    private static final String RESOURCE = "ratelimit/lua/sliding_window_log.lua";

    private final RScript script;
    private final String body;
    private String digest;

    /**
     * 从 classpath 读取脚本并 {@code SCRIPT LOAD}。
     *
     * @param redisson Redisson 客户端，非 null
     */
    public RedissonSlidingWindowLogScript(RedissonClient redisson) {
        Objects.requireNonNull(redisson, "redisson");
        this.script = redisson.getScript(StringCodec.INSTANCE);
        this.body = loadBody();
        this.digest = script.scriptLoad(body);
    }

    /**
     * 执行滑动窗口日志脚本。
     * <p>
     * 幂等：否——脚本会修改 ZSET。
     *
     * @param redisKey ZSET key
     * @param argv     limit、nowMs、windowStart、member、expireMs
     * @return 统一四元组
     */
    public RedissonLuaResult eval(String redisKey, String... argv) {
        Objects.requireNonNull(redisKey, "redisKey");
        Objects.requireNonNull(argv, "argv");
        try {
            return RedissonLuaResult.parse(evalSha(redisKey, argv));
        } catch (RuntimeException ex) {
            if (!isNoScript(ex)) {
                throw ex;
            }
            digest = script.scriptLoad(body);
            return RedissonLuaResult.parse(evalSha(redisKey, argv));
        }
    }

    /**
     * 调用 {@code EVALSHA} 并返回原始列表。
     */
    @SuppressWarnings("unchecked")
    private List<Object> evalSha(String redisKey, String... argv) {
        return script.evalSha(
                RScript.Mode.READ_WRITE,
                digest,
                RScript.ReturnType.MULTI,
                Collections.singletonList(redisKey),
                (Object[]) argv);
    }

    /**
     * 判断是否为 Redis NOSCRIPT（Redisson 包装后仍含关键字）。
     */
    private static boolean isNoScript(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("NOSCRIPT")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 从 classpath 读取 Lua 正文。
     *
     * @return 脚本源码
     */
    private static String loadBody() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = RedissonSlidingWindowLogScript.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("missing Lua resource: " + RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to load " + RESOURCE, e);
        }
    }
}
