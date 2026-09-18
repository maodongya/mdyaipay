package com.mdyaipay.payment.testsupport;

import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.repository.PaymentOrderRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 单测用进程内 Map 仓储，非生产实现。 */
public final class MapPaymentOrderRepository implements PaymentOrderRepository {

    private final Map<String, PaymentOrder> store = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public PaymentOrder save(PaymentOrder order) {
        store.put(order.getOrderNo(), order);
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<PaymentOrder> findByOrderNo(String orderNo) {
        return Optional.ofNullable(store.get(orderNo));
    }
}
