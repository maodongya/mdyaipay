package com.mdyaipay.payment.service;

import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;
import com.mdyaipay.payment.integration.accounting.PaymentCollectAccountingNotifier;
import com.mdyaipay.payment.service.collect.PaymentApplicationService;
import com.mdyaipay.payment.testsupport.MapPaymentOrderRepository;
import com.mdyaipay.payment.testsupport.PaymentTestSupport;
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.logging.LogMessageCapture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付失败路径的稳定事件日志。
 */
class PaymentAlertEventLoggingTest {

    /** 渠道抛错记录 payment_channel_failed。 */
    @Test
    void shouldLogPaymentChannelFailed() {
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                order -> {
                    throw new IllegalStateException("channel down");
                },
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP);
        List<String> messages = LogMessageCapture.capture(PaymentApplicationService.class, () ->
                service.createAndPay("ORDER-CH-FAIL", 100L, "MOCK"));
        assertTrue(contains(messages, AlertEvents.PAYMENT_CHANNEL_FAILED));
    }

    /** 渠道同步失败记录 payment_failed。 */
    @Test
    void shouldLogPaymentFailed() {
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                order -> PaymentSubmitResult.syncFailure(),
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP);
        List<String> messages = LogMessageCapture.capture(PaymentApplicationService.class, () ->
                service.createAndPay("ORDER-FAIL", 100L, "MOCK"));
        assertTrue(contains(messages, AlertEvents.PAYMENT_FAILED));
    }

    /** 账务通知抛错记录 accounting_notify_failed。 */
    @Test
    void shouldLogAccountingNotifyFailed() {
        PaymentCollectAccountingNotifier notifier = order -> {
            throw new IllegalStateException("accounting down");
        };
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                order -> PaymentSubmitResult.syncSuccess("CH-1"),
                PaymentTestSupport.businessNoGenerator(),
                notifier);
        List<String> messages = LogMessageCapture.capture(PaymentApplicationService.class, () ->
                service.createAndPay("ORDER-ACC", 100L, "MOCK"));
        assertTrue(contains(messages, AlertEvents.ACCOUNTING_NOTIFY_FAILED));
    }

    private static boolean contains(List<String> messages, String event) {
        String token = AlertEvents.failure(event);
        return messages.stream().anyMatch(line -> line.contains(token));
    }
}
