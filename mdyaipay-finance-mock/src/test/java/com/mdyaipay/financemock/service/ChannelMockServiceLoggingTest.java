package com.mdyaipay.financemock.service;

import com.mdyaipay.financegateway.api.collect.ChannelCollectCommand;
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.logging.LogMessageCapture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mock 渠道失败事件日志。
 */
class ChannelMockServiceLoggingTest {

    /** 确定性失败单号必须写出 mock_channel_failed。 */
    @Test
    void shouldLogMockChannelFailed() {
        ChannelMockService service = new ChannelMockService();
        String orderNo = failingOrderNo();
        List<String> messages = LogMessageCapture.capture(ChannelMockService.class, () ->
                service.collect(new ChannelCollectCommand(orderNo, 100L, "MOCK", "QUICK_COLLECTION", 1L)));
        assertTrue(messages.stream().anyMatch(line ->
                line.contains(AlertEvents.failure(AlertEvents.MOCK_CHANNEL_FAILED))));
    }

    private static String failingOrderNo() {
        for (int i = 0; i < 1000; i++) {
            String orderNo = "FAIL-" + i;
            if (Math.abs(orderNo.hashCode()) % 10 >= 8) {
                return orderNo;
            }
        }
        throw new IllegalStateException("unable to find failing order no");
    }
}
