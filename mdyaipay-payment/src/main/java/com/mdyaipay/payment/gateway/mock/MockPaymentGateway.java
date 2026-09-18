package com.mdyaipay.payment.gateway.mock;

import com.mdyaipay.payment.gateway.PaymentGateway;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;

/**
 * 收单 Mock 渠道：本地/单测用，同步返回成功或失败。
 */
public class MockPaymentGateway implements PaymentGateway {

    /** {@inheritDoc} 网银返回待渠道确认；其余按单号哈希模拟成败。 */
    @Override
    public PaymentSubmitResult pay(PaymentOrder order) {
        if (order.getProductType() == PaymentProductType.ONLINE_BANKING) {
            return PaymentSubmitResult.AWAITING_CHANNEL_CONFIRMATION;
        }
        boolean ok = Math.abs(order.getOrderNo().hashCode()) % 10 < 8;
        return ok ? PaymentSubmitResult.SYNC_SUCCESS : PaymentSubmitResult.SYNC_FAILURE;
    }
}
