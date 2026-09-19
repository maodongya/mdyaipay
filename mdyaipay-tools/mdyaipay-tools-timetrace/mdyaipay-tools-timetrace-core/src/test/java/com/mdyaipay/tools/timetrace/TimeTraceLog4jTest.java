package com.mdyaipay.tools.timetrace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeTraceLog4jTest {

    @Test
    void defaultListenerLogsFormattedReport() {
        TimeTraceNode root = new TimeTraceNode("demo()");
        root.setDurationNanos(1_000_000L);
        TimeTraceReport report = new TimeTraceReport("demo", root, 1_000_000L, null);

        assertDoesNotThrow(() -> TimeTraceLog4j.defaultListener().onComplete(report));

        String formatted = TimeTraceReportFormatter.format(report);
        assertTrue(formatted.contains("[TimeTrace] demo"));
    }
}
