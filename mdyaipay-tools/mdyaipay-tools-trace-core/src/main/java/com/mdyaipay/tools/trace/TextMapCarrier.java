package com.mdyaipay.tools.trace;

import com.mdyaipay.tools.trace.internal.MutableTextMapCarrier;

/**
 * 键值传播介质（HTTP 头、Dubbo attachment、MQ UserProperty 等）。
 */
public interface TextMapCarrier {

    /**
     * 按 key 读取；不存在时返回 null。
     *
     * @param key 键名（HTTP 场景建议大小写不敏感）
     */
    String get(String key);

    /**
     * 写入键值；只读 Carrier 应抛出 {@link UnsupportedOperationException}。
     */
    default void set(String key, String value) {
        throw new UnsupportedOperationException("只读 Carrier");
    }

    /** 创建可写的内存 Carrier（单测、适配层）。 */
    static TextMapCarrier create() {
        return new MutableTextMapCarrier();
    }
}
