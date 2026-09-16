package com.mdyaipay.payment.service.payout;

import com.mdyaipay.payment.domain.payout.PayoutGateway;
import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.domain.payout.PayoutStatus;
import com.mdyaipay.payment.testsupport.MapPayoutOrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PayoutApplicationServiceTest {

    @Test
    void shouldRemitSuccessWhenGatewayReturnsTrue() {
        PayoutGateway gateway = order -> true;
        PayoutApplicationService service = new PayoutApplicationService(
                new MapPayoutOrderRepository(),
                gateway
        );

        PayoutOrder order = service.createAndRemit("PO-1", 100L, "MOCK", "payee-1");
        Assertions.assertEquals(PayoutStatus.SUCCESS, order.getStatus());
    }

    @Test
    void shouldKeepIdempotentForSamePayoutNo() {
        PayoutGateway gateway = order -> true;
        PayoutApplicationService service = new PayoutApplicationService(
                new MapPayoutOrderRepository(),
                gateway
        );

        PayoutOrder first = service.createAndRemit("PO-2", 100L, "MOCK", "payee-1");
        PayoutOrder second = service.createAndRemit("PO-2", 200L, "MOCK", "payee-2");

        Assertions.assertSame(first, second);
        Assertions.assertEquals(100L, second.getAmount());
    }
}
