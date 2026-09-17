package com.mdyaipay.tools.loadtest.http;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 从场景 {@code target} Map 读取 HTTP 参数；缺省时与设计文档默认一致。 */
final class HttpTargetSupport {

    private HttpTargetSupport() {
    }

    static String url(Map<String, Object> target) {
        Object v = target.get("url");
        if (v == null) {
            throw new IllegalArgumentException("target.url is required");
        }
        return String.valueOf(v);
    }

    static String method(Map<String, Object> target) {
        Object v = target.get("method");
        return v == null ? "GET" : String.valueOf(v).toUpperCase();
    }

    static Duration timeout(Map<String, Object> target) {
        long ms = 5000;
        Object v = target.get("timeoutMillis");
        if (v instanceof Number n) {
            ms = n.longValue();
        }
        return Duration.ofMillis(ms);
    }

    @SuppressWarnings("unchecked")
    static Map<String, String> headers(Map<String, Object> target) {
        Object v = target.get("headers");
        if (!(v instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        raw.forEach((k, val) -> out.put(String.valueOf(k), String.valueOf(val)));
        return out;
    }

    static String body(Map<String, Object> target) {
        Object v = target.get("body");
        return v == null ? null : String.valueOf(v);
    }

    /** 可选：{@code merchantEncryptedCollect} 表示按商户开放 API 构造加密收单 body。 */
    static String bodyMode(Map<String, Object> target) {
        Object v = target.get("bodyMode");
        return v == null ? null : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> merchantEncryptedCollect(Map<String, Object> target) {
        Object v = target.get("merchantEncryptedCollect");
        if (v instanceof Map<?, ?> raw) {
            Map<String, Object> out = new LinkedHashMap<>();
            raw.forEach((k, val) -> out.put(String.valueOf(k), val));
            return out;
        }
        return Map.of();
    }

    /** 信任所有服务端证书；默认 {@code false}。 */
    static boolean insecureTls(Map<String, Object> target) {
        Object v = target.get("insecure");
        return v instanceof Boolean b && b;
    }

    static int maxConnections(Map<String, Object> target) {
        Object v = target.get("maxConnections");
        if (v instanceof Number n) {
            return Math.max(1, n.intValue());
        }
        return 200;
    }

    static List<Integer> expectStatus(Map<String, Object> target) {
        Object v = target.get("expectStatus");
        if (!(v instanceof List<?> list)) {
            return List.of(200);
        }
        List<Integer> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Number n) {
                out.add(n.intValue());
            }
        }
        return out.isEmpty() ? List.of(200) : out;
    }

    static URI toUri(String url) {
        return URI.create(url);
    }
}
