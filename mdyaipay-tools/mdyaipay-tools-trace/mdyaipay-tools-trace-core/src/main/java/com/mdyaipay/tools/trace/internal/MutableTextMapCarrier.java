package com.mdyaipay.tools.trace.internal;

import com.mdyaipay.tools.trace.TextMapCarrier;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 Map Carrier，供单测与 spring-boot 适配层委托。
 */
public final class MutableTextMapCarrier implements TextMapCarrier {

    private final Map<String, String> values = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return values.get(normalized);
    }

    @Override
    public void set(String key, String value) {
        if (key == null) {
            return;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        if (value == null) {
            values.remove(normalized);
        } else {
            values.put(normalized, value);
        }
    }

    /** 只读视图，供断言。 */
    public Map<String, String> asMap() {
        return Map.copyOf(values);
    }
}
