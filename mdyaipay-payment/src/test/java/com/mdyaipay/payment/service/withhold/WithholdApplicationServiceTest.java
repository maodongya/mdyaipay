package com.mdyaipay.payment.service.withhold;

import com.mdyaipay.payment.domain.withhold.WithholdGateway;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.domain.withhold.WithholdStatus;
import com.mdyaipay.payment.testsupport.MapWithholdOrderRepository;
import com.mdyaipay.payment.testsupport.PaymentTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WithholdApplicationServiceTest {

    @Test
    void shouldDeductSuccessWhenGatewayReturnsTrue() {
        WithholdGateway gateway = order -> true;
        WithholdApplicationService service = new WithholdApplicationService(
                new MapWithholdOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        WithholdOrder order = service.createAndDeduct("WH-1", "AGR-1", 100L, "MOCK");
        Assertions.assertEquals(WithholdStatus.SUCCESS, order.getStatus());
    }

    @Test
    void shouldKeepIdempotentForSameDeductionNo() {
        WithholdGateway gateway = order -> true;
        WithholdApplicationService service = new WithholdApplicationService(
                new MapWithholdOrderRepository(),
                gateway,
                PaymentTestSupport.businessNoGenerator()
        );

        WithholdOrder first = service.createAndDeduct("WH-2", "AGR-1", 100L, "MOCK");
        WithholdOrder second = service.createAndDeduct("WH-2", "AGR-9", 999L, "MOCK");

        Assertions.assertSame(first, second);
        Assertions.assertEquals(100L, second.getAmount());
    }
}
