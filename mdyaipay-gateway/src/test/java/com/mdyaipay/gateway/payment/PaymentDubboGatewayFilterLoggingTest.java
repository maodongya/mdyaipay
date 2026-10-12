package com.mdyaipay.gateway.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.logging.LogMessageCapture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 网关上游失败事件日志。
 */
class PaymentDubboGatewayFilterLoggingTest {

    /** Dubbo/下游技术失败必须留下稳定 event 标记。 */
    @Test
    void shouldLogGatewayUpstreamFailed() {
        PaymentDubboGatewayFilter filter = new PaymentDubboGatewayFilter(null, new ObjectMapper());
        List<String> messages = LogMessageCapture.capture(PaymentDubboGatewayFilter.class, () ->
                filter.logUpstreamFailed(new RuntimeException("dubbo down")));
        assertTrue(messages.stream().anyMatch(line ->
                line.contains(AlertEvents.failure(AlertEvents.GATEWAY_UPSTREAM_FAILED))));
    }
}
