package com.mdyaipay.payment.gateway.mock;

import com.mdyaipay.payment.gateway.PayoutGateway;
import com.mdyaipay.payment.domain.payout.PayoutOrder;

/**
 * 代付 Mock 渠道：本地/单测用。
 */
public class MockPayoutGateway implements PayoutGateway {

    /** {@inheritDoc} 按出款单号哈希模拟约 80% 成功。 */
    @Override
    public boolean remit(PayoutOrder order) {
        return Math.abs(order.getPayoutNo().hashCode()) % 10 < 8;
    }
}
