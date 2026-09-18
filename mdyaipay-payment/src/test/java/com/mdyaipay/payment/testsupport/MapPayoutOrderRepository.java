package com.mdyaipay.payment.testsupport;

import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.repository.PayoutOrderRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 单测用进程内 Map 仓储，非生产实现。 */
public final class MapPayoutOrderRepository implements PayoutOrderRepository {

    private final Map<String, PayoutOrder> store = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public PayoutOrder save(PayoutOrder order) {
        store.put(order.getPayoutNo(), order);
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<PayoutOrder> findByPayoutNo(String payoutNo) {
        return Optional.ofNullable(store.get(payoutNo));
    }
}
