package com.mdyaipay.tools.trace.http.gateway;

import com.mdyaipay.tools.trace.autoconfigure.TraceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gateway Trace Filter 顺序约束：须先于典型业务 GlobalFilter（{@code HIGHEST_PRECEDENCE + 10}）执行。
 */
class TraceGatewayGlobalFilterOrderTest {

    /**
     * 默认 order 应小于商户收单等业务 Filter，避免短路链跳过 Trace。
     */
    @Test
    void defaultOrderRunsBeforeTypicalShortCircuitBusinessFilters() {
        TraceProperties properties = new TraceProperties();
        TraceGatewayGlobalFilter filter = new TraceGatewayGlobalFilter(properties);
        int businessCollectOrder = Ordered.HIGHEST_PRECEDENCE + 10;
        assertTrue(
                filter.getOrder() < businessCollectOrder,
                "trace order=" + filter.getOrder() + " must be < business order=" + businessCollectOrder);
    }
}
