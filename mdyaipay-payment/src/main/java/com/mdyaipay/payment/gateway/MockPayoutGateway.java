package com.mdyaipay.payment.gateway;

import com.mdyaipay.payment.domain.payout.PayoutGateway;
import com.mdyaipay.payment.domain.payout.PayoutOrder;

public class MockPayoutGateway implements PayoutGateway {
    @Override
    public boolean remit(PayoutOrder order) {
        return Math.abs(order.getPayoutNo().hashCode()) % 10 < 8;
    }
}
