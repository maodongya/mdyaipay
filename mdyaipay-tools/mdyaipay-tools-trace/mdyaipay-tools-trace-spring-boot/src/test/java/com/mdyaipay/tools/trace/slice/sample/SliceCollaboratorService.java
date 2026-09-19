package com.mdyaipay.tools.trace.slice.sample;

import com.mdyaipay.tools.trace.TraceContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用下游 Bean，记录执行时的 spanId。
 */
@Service
public class SliceCollaboratorService {

    private final List<String> spanIds = new ArrayList<>();

    /**
     * 返回当前 spanId 并缓存，供断言嵌套层级。
     */
    public String recordSpan() {
        String spanId = TraceContext.current().map(s -> s.spanId()).orElse(null);
        spanIds.add(spanId);
        return spanId;
    }

    /** 本 Bean 被调用时记录的 spanId 列表（测试只读）。 */
    List<String> recordedSpanIds() {
        return List.copyOf(spanIds);
    }

    /** 测试重置状态。 */
    public void reset() {
        spanIds.clear();
    }
}
