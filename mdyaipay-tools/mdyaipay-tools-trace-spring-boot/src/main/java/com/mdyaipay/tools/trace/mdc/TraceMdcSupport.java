package com.mdyaipay.tools.trace.mdc;

import com.mdyaipay.tools.trace.TraceSnapshot;
import com.mdyaipay.tools.trace.autoconfigure.TraceProperties;
import org.slf4j.MDC;

import java.util.Optional;

/**
 * 将 {@link TraceSnapshot} 写入 SLF4J MDC，供 Log4j2 等布局引用 {@code %X{traceId}}。
 */
public final class TraceMdcSupport {

    private final TraceProperties.Mdc mdc;

    public TraceMdcSupport(TraceProperties properties) {
        this.mdc = properties.getMdc();
    }

    /**
     * 绑定 MDC；未启用 MDC 时不操作。
     */
    public void put(TraceSnapshot snapshot) {
        if (!mdc.isEnabled() || snapshot == null) {
            return;
        }
        MDC.put(mdc.getTraceIdKey(), snapshot.traceId());
        MDC.put(mdc.getSpanIdKey(), snapshot.spanId());
    }

    /** 清除本组件写入的 MDC 键。 */
    public void clear() {
        if (!mdc.isEnabled()) {
            return;
        }
        MDC.remove(mdc.getTraceIdKey());
        MDC.remove(mdc.getSpanIdKey());
    }

    /**
     * 嵌套切片结束后恢复 MDC；无外层快照时等价于 {@link #clear()}。
     */
    public void restore(Optional<TraceSnapshot> previous) {
        if (!mdc.isEnabled()) {
            return;
        }
        if (previous == null || previous.isEmpty()) {
            clear();
        } else {
            put(previous.get());
        }
    }
}
