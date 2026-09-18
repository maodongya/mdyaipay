package com.mdyaipay.tools.model;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceIds;
import com.mdyaipay.tools.trace.TraceSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiResponseTraceTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    /**
     * 有 TraceContext 时响应 traceId 为完整 traceId 的后 12 位。
     */
    @Test
    void usesDisplayTraceIdFromContext() {
        TraceSnapshot snapshot = TraceSnapshot.startNew();
        TraceContext.bind(snapshot);

        ApiResponse<String> response = ApiResponse.ok("x");
        assertEquals(TraceIds.toDisplayTraceId(snapshot.traceId()), response.getTraceId());
    }
}
