package com.mdyaipay.tools.ratelimit.redis;

import java.util.List;
import java.util.Objects;

/**
 * Lua 脚本统一四元组返回值：allowed / remaining / limit / 第四字段。
 * <p>
 * 多数算法第四字段为 {@code retryAfterMs}；{@link RateLimitAlgorithm#SLIDING_WINDOW_LOG}
 * 拒绝时为最旧 ZSET score（毫秒），由 Java 换算 retryAfter。
 *
 * @param allowed      是否允许（Lua 1/0）
 * @param remaining    剩余配额
 * @param limit        策略上限
 * @param retryAfterMs 建议重试毫秒，或滑动日志拒绝时的最旧 score
 */
public record RedisLuaResult(boolean allowed, long remaining, long limit, long retryAfterMs) {

    /**
     * 从 Lettuce MULTI 输出解析四元组。
     *
     * @param values 长度至少 4 的数字列表
     * @return 解析结果
     */
    public static RedisLuaResult parse(List<?> values) {
        Objects.requireNonNull(values, "values");
        if (values.size() < 4) {
            throw new IllegalArgumentException("Lua result must have 4 elements");
        }
        return new RedisLuaResult(
                toLong(values.get(0)) == 1L,
                toLong(values.get(1)),
                toLong(values.get(2)),
                toLong(values.get(3)));
    }

    /**
     * 将 Lua 数字（Long/Integer/String）转为 long。
     */
    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("unsupported Lua number type: " + value);
    }
}
