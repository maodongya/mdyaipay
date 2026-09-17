package com.mdyaipay.payment.gateway;

import com.mdyaipay.payment.domain.payout.PayoutGateway;
import com.mdyaipay.payment.domain.payout.PayoutOrder;

/**
 * 代付 Mock 渠道：本地/单测用。
 */
public class MockPayoutGateway implements PayoutGateway {
    @Override
    public boolean remit(PayoutOrder order) {
        return Math.abs(order.getPayoutNo().hashCode()) % 10 < 8;
    }
}
