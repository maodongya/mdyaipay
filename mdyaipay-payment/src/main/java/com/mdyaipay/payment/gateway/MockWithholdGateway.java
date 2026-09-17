package com.mdyaipay.payment.gateway;

import com.mdyaipay.payment.domain.withhold.WithholdGateway;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;

/**
 * 代扣 Mock 渠道：本地/单测用。
 */
public class MockWithholdGateway implements WithholdGateway {
    @Override
    public boolean deduct(WithholdOrder order) {
        return Math.abs(order.getDeductionNo().hashCode()) % 10 < 8;
    }
}
