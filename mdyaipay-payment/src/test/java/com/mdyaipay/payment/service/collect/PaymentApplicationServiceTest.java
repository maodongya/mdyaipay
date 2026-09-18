package com.mdyaipay.payment.service.collect;

import com.mdyaipay.payment.gateway.PaymentGateway;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;
import com.mdyaipay.payment.testsupport.MapPaymentOrderRepository;
import com.mdyaipay.payment.testsupport.PaymentTestSupport;
import com.mdyaipay.payment.gateway.mock.MockPaymentGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 收单应用服务单测：同步成功、网银待确认、幂等与非法金额。
 */
class PaymentApplicationServiceTest {

    /** 快捷收单渠道同步成功则订单 SUCCESS。 */
    @Test
    void shouldPaySuccessWhenGatewayReturnsSyncSuccess() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        PaymentOrder order = service.createAndPay("ORDER-1", 100L, "MOCK");
        Assertions.assertEquals(PaymentStatus.SUCCESS, order.getStatus());
        Assertions.assertEquals(PaymentProductType.QUICK_COLLECTION, order.getProductType());
    }

    /** 网银收单保持 PROCESSING 直至渠道确认。 */
    @Test
    void shouldStayProcessingWhenOnlineBankingAwaitingChannel() {
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                new MockPaymentGateway(),
                PaymentTestSupport.businessNoGenerator()
        );

        PaymentOrder order = service.createAndPay("ORDER-BANK-1", 100L, "MOCK", PaymentProductType.ONLINE_BANKING);
        Assertions.assertEquals(PaymentProductType.ONLINE_BANKING, order.getProductType());
        Assertions.assertEquals(PaymentStatus.PROCESSING, order.getStatus());
    }

    /** 网银回调成功将订单迁到 SUCCESS。 */
    @Test
    void shouldConfirmOnlineBankingChannelSuccess() {
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                new MockPaymentGateway(),
                PaymentTestSupport.businessNoGenerator()
        );

        service.createAndPay("ORDER-BANK-2", 100L, "MOCK", PaymentProductType.ONLINE_BANKING);
        PaymentOrder confirmed = service.confirmChannelPayment("ORDER-BANK-2", true);
        Assertions.assertEquals(PaymentStatus.SUCCESS, confirmed.getStatus());
    }

    /** 确认不存在的订单抛 IllegalArgumentException。 */
    @Test
    void shouldRejectConfirmWhenOrderNotFound() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                service.confirmChannelPayment("MISSING", true));
    }

    /** 快捷单不允许走渠道确认。 */
    @Test
    void shouldRejectConfirmWhenNotOnlineBanking() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        service.createAndPay("ORDER-QUICK-X", 100L, "MOCK");
        Assertions.assertThrows(IllegalStateException.class, () ->
                service.confirmChannelPayment("ORDER-QUICK-X", true));
    }

    /** 金额非法时拒绝下单。 */
    @Test
    void shouldThrowWhenAmountInvalid() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                service.createAndPay("ORDER-2", 0L, "MOCK"));
    }

    /** 同一 orderNo 再次下单返回原单。 */
    @Test
    void shouldKeepIdempotentForSameOrderNo() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        PaymentOrder first = service.createAndPay("ORDER-3", 100L, "MOCK");
        PaymentOrder second = service.createAndPay("ORDER-3", 200L, "MOCK");

        Assertions.assertSame(first, second);
        Assertions.assertEquals(100L, second.getAmount());
    }

    /** 未传 orderNo 时分配雪花单号。 */
    @Test
    void shouldAssignSnowflakeOrderNoWhenClientOmitsOrderNo() {
        PaymentGateway gateway = order -> PaymentSubmitResult.SYNC_SUCCESS;
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        PaymentOrder order = service.createAndPay(null, 100L, "MOCK");
        Assertions.assertTrue(order.getOrderNo().matches("\\d{16,20}"));
    }
}
