package com.mdyaipay.tools.ratelimit.redisson;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RedissonLuaResult} 解析单元测。
 */
class RedissonLuaResultTest {

    /**
     * 解析允许态四元组。
     */
    @Test
    void parseAllowed() {
        RedissonLuaResult r = RedissonLuaResult.parse(List.of(1L, 9L, 10L, 0L));
        assertTrue(r.allowed());
        assertEquals(9L, r.remaining());
        assertEquals(10L, r.limit());
        assertEquals(0L, r.retryAfterMs());
    }

    /**
     * 解析拒绝态四元组。
     */
    @Test
    void parseDenied() {
        RedissonLuaResult r = RedissonLuaResult.parse(List.of(0L, 0L, 5L, 1_700_000_000_000L));
        assertFalse(r.allowed());
        assertEquals(1_700_000_000_000L, r.retryAfterMs());
    }
}
