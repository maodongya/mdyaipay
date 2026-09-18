package com.mdyaipay.tools.trace.internal;

import com.mdyaipay.tools.trace.TraceIds;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * W3C {@code traceparent}（version {@code 00}）编解码。
 */
public final class TraceparentCodec {

    private static final String VERSION = "00";
    private static final Pattern PATTERN = Pattern.compile(
            "^(?<version>[0-9a-f]{2})-(?<traceId>[0-9a-f]{32})-(?<spanId>[0-9a-f]{16})-(?<flags>[0-9a-f]{2})$");

    private TraceparentCodec() {
    }

    public static String format(String traceId, String spanId, boolean sampled) {
        TraceIds.requireTraceId(traceId);
        TraceIds.requireSpanId(spanId);
        int flags = sampled ? 0x01 : 0x00;
        return VERSION + "-" + traceId + "-" + spanId + "-" + String.format(Locale.ROOT, "%02x", flags);
    }

    public static Parsed parse(String header) {
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("traceparent 为空");
        }
        Matcher matcher = PATTERN.matcher(header.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("traceparent 格式非法");
        }
        if (!VERSION.equals(matcher.group("version"))) {
            throw new IllegalArgumentException("不支持的 traceparent version");
        }
        String traceId = matcher.group("traceId");
        String spanId = matcher.group("spanId");
        TraceIds.requireTraceId(traceId);
        TraceIds.requireSpanId(spanId);
        boolean sampled = (Integer.parseInt(matcher.group("flags"), 16) & 0x01) == 0x01;
        return new Parsed(traceId, spanId, sampled);
    }

    /** 自 inbound {@code traceparent} 解析出的远端 span 信息。 */
    public record Parsed(String traceId, String spanId, boolean sampled) {
    }
}
