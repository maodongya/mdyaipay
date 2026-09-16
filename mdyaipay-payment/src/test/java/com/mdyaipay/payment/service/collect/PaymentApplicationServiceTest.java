package com.mdyaipay.payment.service.collect;

import com.mdyaipay.payment.domain.collect.PaymentGateway;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;
import com.mdyaipay.payment.testsupport.MapPaymentOrderRepository;
import com.mdyaipay.payment.gateway.MockPaymentGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PaymentApplicationServiceTest {

    @Test
    void shouldPaySuccessWhenGatewayReturnsSyncSuccess() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway
        );

        PaymentOrder order = service.createAndPay("ORDER-1", 100L, "MOCK");
        Assertions.assertEquals(PaymentStatus.SUCCESS, order.getStatus());
        Assertions.assertEquals(PaymentProductType.QUICK_COLLECTION, order.getProductType());
    }

    @Test
    void shouldStayProcessingWhenOnlineBankingAwaitingChannel() {
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                new MockPaymentGateway()
        );

        PaymentOrder order = service.createAndPay("ORDER-BANK-1", 100L, "MOCK", PaymentProductType.ONLINE_BANKING);
        Assertions.assertEquals(PaymentProductType.ONLINE_BANKING, order.getProductType());
        Assertions.assertEquals(PaymentStatus.PROCESSING, order.getStatus());
    }

    @Test
    void shouldConfirmOnlineBankingChannelSuccess() {
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                new MockPaymentGateway()
        );

        service.createAndPay("ORDER-BANK-2", 100L, "MOCK", PaymentProductType.ONLINE_BANKING);
        PaymentOrder confirmed = service.confirmChannelPayment("ORDER-BANK-2", true);
        Assertions.assertEquals(PaymentStatus.SUCCESS, confirmed.getStatus());
    }

    @Test
    void shouldRejectConfirmWhenOrderNotFound() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway
        );

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                service.confirmChannelPayment("MISSING", true));
    }

    @Test
    void shouldRejectConfirmWhenNotOnlineBanking() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway
        );

        service.createAndPay("ORDER-QUICK-X", 100L, "MOCK");
        Assertions.assertThrows(IllegalStateException.class, () ->
                service.confirmChannelPayment("ORDER-QUICK-X", true));
    }

    @Test
    void shouldThrowWhenAmountInvalid() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway
        );

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                service.createAndPay("ORDER-2", 0L, "MOCK"));
    }

    @Test
    void shouldKeepIdempotentForSameOrderNo() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway
        );

        PaymentOrder first = service.createAndPay("ORDER-3", 100L, "MOCK");
        PaymentOrder second = service.createAndPay("ORDER-3", 200L, "MOCK");

        Assertions.assertSame(first, second);
        Assertions.assertEquals(100L, second.getAmount());
    }
}
