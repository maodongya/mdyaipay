package com.mdyaipay.payment.testsupport;

import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.domain.payout.PayoutOrderRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MapPayoutOrderRepository implements PayoutOrderRepository {

    private final Map<String, PayoutOrder> store = new ConcurrentHashMap<>();

    @Override
    public PayoutOrder save(PayoutOrder order) {
        store.put(order.getPayoutNo(), order);
        return order;
    }

    @Override
    public Optional<PayoutOrder> findByPayoutNo(String payoutNo) {
        return Optional.ofNullable(store.get(payoutNo));
    }
}
