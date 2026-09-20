package com.mdyaipay.tools.ratelimit.redis;

import com.mdyaipay.tools.ratelimit.redis.script.RateLimitScripts;
import io.lettuce.core.RedisNoScriptException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lua 脚本装载与执行：优先 EVALSHA，遇 NOSCRIPT 降级 EVAL 并刷新 SHA。
 * <p>
 * <b>不负责</b> 连接生命周期——由调用方持有 {@link StatefulRedisConnection}。
 */
public final class RedisScriptSupport {

    private final RedisCommands<String, String> sync;
    private final Map<RateLimitScripts, String> bodies = new EnumMap<>(RateLimitScripts.class);
    private final Map<RateLimitScripts, String> digests = new EnumMap<>(RateLimitScripts.class);

    /**
     * 装载全部限流脚本并 SCRIPT LOAD。
     *
     * @param connection 已连接的 Redis，非 null
     */
    public RedisScriptSupport(StatefulRedisConnection<String, String> connection) {
        Objects.requireNonNull(connection, "connection");
        this.sync = connection.sync();
        for (RateLimitScripts script : RateLimitScripts.values()) {
            String body = script.loadBody();
            bodies.put(script, body);
            digests.put(script, sync.scriptLoad(body));
        }
    }

    /**
     * 读取 Redis 服务器时钟（{@code TIME}），用于脚本参数预计算。
     *
     * @return 时刻快照
     */
    public RedisServerTime serverTime() {
        return RedisServerTime.read(sync);
    }

    /**
     * 执行限流脚本。
     * <p>
     * 幂等：否——脚本可能修改 Redis 状态。
     *
     * @param script 脚本枚举
     * @param key    Redis key（部分算法由 Java 拼好完整 key）
     * @param argv   脚本参数
     * @return 统一四元组
     */
    public RedisLuaResult eval(RateLimitScripts script, String key, String... argv) {
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(argv, "argv");
        try {
            @SuppressWarnings("unchecked")
            List<Object> raw = sync.evalsha(digests.get(script), ScriptOutputType.MULTI, new String[]{key}, argv);
            return RedisLuaResult.parse(raw);
        } catch (RedisNoScriptException ex) {
            String body = bodies.get(script);
            digests.put(script, sync.scriptLoad(body));
            @SuppressWarnings("unchecked")
            List<Object> raw = sync.eval(body, ScriptOutputType.MULTI, new String[]{key}, argv);
            return RedisLuaResult.parse(raw);
        }
    }
}
