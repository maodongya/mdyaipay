package com.mdyaipay.payment.gateway;

import com.mdyaipay.payment.domain.withhold.WithholdGateway;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;

public class MockWithholdGateway implements WithholdGateway {
    @Override
    public boolean deduct(WithholdOrder order) {
        return Math.abs(order.getDeductionNo().hashCode()) % 10 < 8;
    }
}
