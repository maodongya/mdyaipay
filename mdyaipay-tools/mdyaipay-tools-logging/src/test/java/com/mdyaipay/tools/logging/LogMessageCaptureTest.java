package com.mdyaipay.tools.logging;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证日志消息捕获器。
 */
class LogMessageCaptureTest {

    /** 回调内写出的消息必须能被收集。 */
    @Test
    void shouldCaptureLoggerMessage() {
        List<String> messages = LogMessageCapture.capture(LogMessageCaptureTest.class, () ->
                LogManager.getLogger(LogMessageCaptureTest.class).error("event=demo outcome=failure"));
        assertTrue(messages.stream().anyMatch(line -> line.contains("event=demo outcome=failure")));
    }

    /** 空参数拒绝捕获，避免静默漏断言。 */
    @Test
    void shouldRejectNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> LogMessageCapture.capture(null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () ->
                LogMessageCapture.capture(LogMessageCaptureTest.class, null));
    }
}
