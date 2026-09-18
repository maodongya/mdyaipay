package com.mdyaipay.payment.service.payout;

import com.mdyaipay.payment.gateway.PayoutGateway;
import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.domain.payout.PayoutStatus;
import com.mdyaipay.payment.testsupport.MapPayoutOrderRepository;
import com.mdyaipay.payment.testsupport.PaymentTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 代付应用服务单测：渠道成功与单号幂等。
 */
class PayoutApplicationServiceTest {

    /** 渠道打款成功则代付单 SUCCESS。 */
    @Test
    void shouldRemitSuccessWhenGatewayReturnsTrue() {
        PayoutGateway gateway = order -> true;
        PayoutApplicationService service = new PayoutApplicationService(
                new MapPayoutOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        PayoutOrder order = service.createAndRemit("PO-1", 100L, "MOCK", "payee-1");
        Assertions.assertEquals(PayoutStatus.SUCCESS, order.getStatus());
    }

    /** 同一 payoutNo 再次代付返回原单。 */
    @Test
    void shouldKeepIdempotentForSamePayoutNo() {
        PayoutGateway gateway = order -> true;
        PayoutApplicationService service = new PayoutApplicationService(
                new MapPayoutOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        PayoutOrder first = service.createAndRemit("PO-2", 100L, "MOCK", "payee-1");
        PayoutOrder second = service.createAndRemit("PO-2", 200L, "MOCK", "payee-2");

        Assertions.assertSame(first, second);
        Assertions.assertEquals(100L, second.getAmount());
    }
}
