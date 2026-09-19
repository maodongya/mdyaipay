package com.mdyaipay.tools.trace.http.servlet;

import com.mdyaipay.tools.trace.TraceSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * {@link ServletTraceEntryLog}  smoke：无 Spring 容器，仅验证调用不抛错。
 */
class ServletTraceEntryLogTest {

    /**
     * 关闭开关时不写日志、不抛错。
     */
    @Test
    void logDisabledIsNoOp() {
        TraceSnapshot root = TraceSnapshot.startNew();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/merchants/members/assert");
        ServletTraceEntryLog.logIfEnabled(false, root, request, System.nanoTime(), null);
    }
}
