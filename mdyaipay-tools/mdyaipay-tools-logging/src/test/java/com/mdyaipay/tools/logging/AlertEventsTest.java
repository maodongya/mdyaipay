package com.mdyaipay.tools.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证报警事件标记格式。
 */
class AlertEventsTest {

    /** 失败消息必须同时包含稳定动作名与 outcome。 */
    @Test
    void shouldFormatFailureEvent() {
        assertEquals(
                "event=payment_failed outcome=failure",
                AlertEvents.failure(AlertEvents.PAYMENT_FAILED));
    }

    /** 空动作名拒绝组装，避免产生无法检索的规则。 */
    @Test
    void shouldRejectBlankAction() {
        assertThrows(IllegalArgumentException.class, () -> AlertEvents.failure(" "));
    }
}
