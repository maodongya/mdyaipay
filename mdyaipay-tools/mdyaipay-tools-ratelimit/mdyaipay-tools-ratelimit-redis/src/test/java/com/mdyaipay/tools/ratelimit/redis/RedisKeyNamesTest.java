package com.mdyaipay.tools.ratelimit.redis;

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RedisKeyNames} 与 {@link RedisLuaResult} 纯单元测试。
 */
class RedisKeyNamesTest {

    /**
     * 验证限流 Redis key 前缀与算法段拼装。
     */
    @Test
    void buildsPrefixedKey() {
        assertEquals(
                "mdyaipay:rl:TOKEN_BUCKET:merchant:mk_1",
                RedisKeyNames.of(RateLimitAlgorithm.TOKEN_BUCKET, "merchant:mk_1"));
    }

    /**
     * 验证 Lua 四元组解析为判定字段。
     */
    @Test
    void parsesLuaTuple() {
        RedisLuaResult r = RedisLuaResult.parse(List.of(1L, 9L, 10L, 0L));
        assertTrue(r.allowed());
        assertEquals(9, r.remaining());
        assertEquals(10, r.limit());
        assertEquals(0, r.retryAfterMs());
    }
}
