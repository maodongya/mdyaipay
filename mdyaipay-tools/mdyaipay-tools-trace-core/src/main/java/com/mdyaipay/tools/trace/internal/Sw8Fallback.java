package com.mdyaipay.tools.trace.internal;

import com.mdyaipay.tools.trace.TraceIds;
import com.mdyaipay.tools.trace.TraceSnapshot;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 无 {@code traceparent} 时从 {@code sw8} 尽力恢复 traceId，并<strong>原样透传</strong> sw8。
 * <p>
 * 完整 sw8 语义由 SkyWalking Agent 负责；此处不构造 Segment。
 */
public final class Sw8Fallback {

    private static final Pattern HEX_TRACE = Pattern.compile("^[0-9a-f]{32}$");

    private Sw8Fallback() {
    }

    public static Optional<TraceSnapshot> continueTrace(String sw8) {
        if (sw8 == null || sw8.isBlank()) {
            return Optional.empty();
        }
        String raw = sw8.trim();
        Optional<String> traceId = findTraceId(raw);
        if (traceId.isEmpty()) {
            return Optional.empty();
        }
        TraceSnapshot snapshot = TraceSnapshot.of(
                traceId.get(),
                IdGenerator.newSpanId(),
                null,
                true);
        return Optional.of(snapshot.withSw8(raw));
    }

    private static Optional<String> findTraceId(String sw8) {
        for (String part : sw8.split("-")) {
            if (part.isEmpty()) {
                continue;
            }
            String lower = part.toLowerCase(Locale.ROOT);
            if (HEX_TRACE.matcher(lower).matches()) {
                try {
                    TraceIds.requireTraceId(lower);
                    return Optional.of(lower);
                } catch (IllegalArgumentException ignored) {
                    // 尝试下一段
                }
            }
        }
        return Optional.empty();
    }
}
