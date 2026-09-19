package com.mdyaipay.tools.trace.slice.sample;

import com.mdyaipay.tools.trace.TraceContext;
import org.springframework.stereotype.Service;

/**
 * 测试用入口 Service：调用 Collaborator 形成两层 span。
 */
@Service
public class SliceSampleService {

    private final SliceCollaboratorService collaborator;

    public SliceSampleService(SliceCollaboratorService collaborator) {
        this.collaborator = collaborator;
    }

    /**
     * 返回 [入口 spanId, 下游 spanId]。
     */
    public String[] runFlow() {
        String entrySpan = TraceContext.current().map(s -> s.spanId()).orElse(null);
        String nestedSpan = collaborator.recordSpan();
        return new String[] {entrySpan, nestedSpan};
    }
}
