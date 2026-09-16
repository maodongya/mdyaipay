package com.mdyaipay.payment.gateway;

import com.mdyaipay.payment.domain.collect.PaymentGateway;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;

public class MockPaymentGateway implements PaymentGateway {
    @Override
    public PaymentSubmitResult pay(PaymentOrder order) {
        if (order.getProductType() == PaymentProductType.ONLINE_BANKING) {
            return PaymentSubmitResult.AWAITING_CHANNEL_CONFIRMATION;
        }
        boolean ok = Math.abs(order.getOrderNo().hashCode()) % 10 < 8;
        return ok ? PaymentSubmitResult.SYNC_SUCCESS : PaymentSubmitResult.SYNC_FAILURE;
    }
}
