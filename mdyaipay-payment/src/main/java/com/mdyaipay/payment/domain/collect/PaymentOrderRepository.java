package com.mdyaipay.payment.domain.collect;

import java.util.Optional;

public interface PaymentOrderRepository {
    PaymentOrder save(PaymentOrder order);

    Optional<PaymentOrder> findByOrderNo(String orderNo);
}
