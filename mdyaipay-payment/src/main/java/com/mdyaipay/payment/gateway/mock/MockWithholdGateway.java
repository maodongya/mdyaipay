package com.mdyaipay.payment.gateway.mock;

import com.mdyaipay.payment.gateway.WithholdGateway;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;

/**
 * 代扣 Mock 渠道：本地/单测用。
 */
public class MockWithholdGateway implements WithholdGateway {

    /** {@inheritDoc} 按代扣单号哈希模拟约 80% 成功。 */
    @Override
    public boolean deduct(WithholdOrder order) {
        return Math.abs(order.getDeductionNo().hashCode()) % 10 < 8;
    }
}
