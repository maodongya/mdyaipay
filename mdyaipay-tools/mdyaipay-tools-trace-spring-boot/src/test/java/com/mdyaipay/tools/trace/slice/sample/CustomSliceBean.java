package com.mdyaipay.tools.trace.slice.sample;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.slice.TraceSlice;
import org.springframework.stereotype.Component;

/**
 * 无 stereotype 扩展场景：仅用 {@link TraceSlice} 标记的 Bean（测试用）。
 */
@Component
@TraceSlice
public class CustomSliceBean {

    /**
     * 返回当前 spanId；无 Trace 上下文时为 null。
     */
    public String currentSpanId() {
        return TraceContext.current().map(s -> s.spanId()).orElse(null);
    }
}
