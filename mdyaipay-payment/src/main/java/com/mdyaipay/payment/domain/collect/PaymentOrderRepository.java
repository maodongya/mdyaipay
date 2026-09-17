package com.mdyaipay.payment.domain.collect;

import java.util.Optional;

/**
 * 收单聚合持久化端口（按 {@code orderNo} 幂等查询）。
 */
public interface PaymentOrderRepository {
    PaymentOrder save(PaymentOrder order);

    Optional<PaymentOrder> findByOrderNo(String orderNo);
}
