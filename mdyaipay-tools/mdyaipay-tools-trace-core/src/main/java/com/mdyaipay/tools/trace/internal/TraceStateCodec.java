package com.mdyaipay.tools.trace.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code tracestate} 的简单 {@code key=value} 编解码（非 PII，条数与长度受限）。
 */
public final class TraceStateCodec {

    static final int MAX_KEYS = 8;
    static final int MAX_VALUE_LENGTH = 256;

    private TraceStateCodec() {
    }

    public static String encode(Map<String, String> baggage) {
        if (baggage == null || baggage.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, String> entry : baggage.entrySet()) {
            if (count >= MAX_KEYS) {
                break;
            }
            String key = trimKey(entry.getKey());
            String value = trimValue(entry.getValue());
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            if (count > 0) {
                builder.append(',');
            }
            builder.append(key).append('=').append(value);
            count++;
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    public static Map<String, String> decode(String traceState) {
        if (traceState == null || traceState.isBlank()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : traceState.split(",")) {
            if (result.size() >= MAX_KEYS) {
                break;
            }
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimKey(pair.substring(0, eq));
            String value = trimValue(pair.substring(eq + 1));
            if (!key.isEmpty() && !value.isEmpty()) {
                result.put(key, value);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static String trimKey(String key) {
        if (key == null) {
            return "";
        }
        String trimmed = key.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    private static String trimValue(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > MAX_VALUE_LENGTH ? trimmed.substring(0, MAX_VALUE_LENGTH) : trimmed;
    }
}
