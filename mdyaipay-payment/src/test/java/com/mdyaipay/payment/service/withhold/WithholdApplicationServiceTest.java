package com.mdyaipay.payment.service.withhold;

import com.mdyaipay.payment.gateway.WithholdGateway;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.domain.withhold.WithholdStatus;
import com.mdyaipay.payment.testsupport.MapWithholdOrderRepository;
import com.mdyaipay.payment.testsupport.PaymentTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 代扣应用服务单测：渠道成功与单号幂等。
 */
class WithholdApplicationServiceTest {

    /** 渠道扣款成功则代扣单 SUCCESS。 */
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

    /** 同一 deductionNo 再次代扣返回原单。 */
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
