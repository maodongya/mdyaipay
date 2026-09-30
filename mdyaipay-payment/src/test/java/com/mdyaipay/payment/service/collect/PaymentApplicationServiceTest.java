package com.mdyaipay.payment.service.collect;

import com.mdyaipay.payment.gateway.PaymentGateway;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;
import com.mdyaipay.payment.testsupport.MapPaymentOrderRepository;
import com.mdyaipay.payment.testsupport.PaymentTestSupport;
import com.mdyaipay.payment.gateway.mock.MockPaymentGateway;
import com.mdyaipay.payment.integration.accounting.PaymentCollectAccountingNotifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 收单应用服务单测：同步成功、网银待确认、幂等与非法金额。
 */
class PaymentApplicationServiceTest {

    /** 快捷收单渠道同步成功则订单 SUCCESS。 */
    @Test
    void shouldPaySuccessWhenGatewayReturnsSyncSuccess() {
        PaymentGateway gateway = order -> PaymentSubmitResult.syncSuccess("CH-ORDER-1");
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
        );

        PaymentOrder order = service.createAndPay("ORDER-1", 100L, "MOCK");
        Assertions.assertEquals(PaymentStatus.SUCCESS, order.getStatus());
        Assertions.assertEquals("CH-ORDER-1", order.getChannelTradeNo());
        Assertions.assertEquals(PaymentProductType.QUICK_COLLECTION, order.getProductType());
    }

    /** 网银收单保持 PROCESSING 直至渠道确认。 */
    @Test
    void shouldStayProcessingWhenOnlineBankingAwaitingChannel() {
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                new MockPaymentGateway(),
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
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
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
        );

        service.createAndPay("ORDER-BANK-2", 100L, "MOCK", PaymentProductType.ONLINE_BANKING);
        PaymentOrder confirmed = service.confirmChannelPayment("ORDER-BANK-2", true, "CH-BANK-2");
        Assertions.assertEquals(PaymentStatus.SUCCESS, confirmed.getStatus());
        Assertions.assertEquals("CH-BANK-2", confirmed.getChannelTradeNo());
    }

    /** 确认不存在的订单抛 IllegalArgumentException。 */
    @Test
    void shouldRejectConfirmWhenOrderNotFound() {
        PaymentGateway gateway = order -> PaymentSubmitResult.syncSuccess("CH-ORDER-2");
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
        );

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                service.confirmChannelPayment("MISSING", true, "CH-MISSING"));
    }

    /** 快捷单不允许走渠道确认。 */
    @Test
    void shouldRejectConfirmWhenNotOnlineBanking() {
        PaymentGateway gateway = order -> PaymentSubmitResult.syncSuccess("CH-QUICK-X");
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
        );

        service.createAndPay("ORDER-QUICK-X", 100L, "MOCK");
        Assertions.assertThrows(IllegalStateException.class, () ->
                service.confirmChannelPayment("ORDER-QUICK-X", true, "CH-QUICK-X"));
    }

    /** 金额非法时拒绝下单。 */
    @Test
    void shouldThrowWhenAmountInvalid() {
        PaymentGateway gateway = order -> PaymentSubmitResult.syncSuccess("CH-ORDER-2");
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
        );

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                service.createAndPay("ORDER-2", 0L, "MOCK"));
    }

    /** 同一 orderNo 再次下单返回原单。 */
    @Test
    void shouldKeepIdempotentForSameOrderNo() {
        PaymentGateway gateway = order -> PaymentSubmitResult.syncSuccess("CH-ORDER-2");
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
        );

        PaymentOrder first = service.createAndPay("ORDER-3", 100L, "MOCK");
        PaymentOrder second = service.createAndPay("ORDER-3", 200L, "MOCK");

        Assertions.assertSame(first, second);
        Assertions.assertEquals(100L, second.getAmount());
    }

    /** 未传 orderNo 时分配雪花单号。 */
    @Test
    void shouldAssignSnowflakeOrderNoWhenClientOmitsOrderNo() {
        PaymentGateway gateway = order -> PaymentSubmitResult.syncSuccess("CH-ORDER-2");
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
        );

        PaymentOrder order = service.createAndPay(null, 100L, "MOCK");
        Assertions.assertTrue(order.getOrderNo().matches("\\d{16,20}"));
    }

    /** 渠道同步成功但没有渠道交易号时，订单记失败且不留下交易号。 */
    @Test
    void shouldFailOrderWhenSyncSuccessLacksChannelTradeNo() {
        PaymentGateway gateway = order -> PaymentSubmitResult.syncSuccess(" ");
        PaymentApplicationService service = new PaymentApplicationService(
                new MapPaymentOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
        );
        PaymentOrder order = service.createAndPay("ORDER-NO-CH", 100L, "MOCK");
        Assertions.assertEquals(PaymentStatus.FAILED, order.getStatus());
        Assertions.assertNull(order.getChannelTradeNo());
    }

    /** 只列出该渠道、该业务日、已成功且带渠道交易号的收单。 */
    @Test
    void shouldListCollectSuccessForChannelAndBusinessDate() {
        MapPaymentOrderRepository repository = new MapPaymentOrderRepository();
        PaymentApplicationService service = new PaymentApplicationService(
                repository,
                order -> PaymentSubmitResult.syncSuccess("CH-" + order.getOrderNo()),
                PaymentTestSupport.businessNoGenerator(),
                PaymentCollectAccountingNotifier.NOOP
        );
        service.createAndPay("ORDER-A", 100L, "MOCK");
        service.createAndPay("ORDER-B", 200L, "OTHER");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));

        List<PaymentOrder> listed = service.listCollectSuccess("MOCK", today);

        Assertions.assertEquals(1, listed.size());
        Assertions.assertEquals("ORDER-A", listed.get(0).getOrderNo());
        Assertions.assertEquals("CH-ORDER-A", listed.get(0).getChannelTradeNo());
    }
}
