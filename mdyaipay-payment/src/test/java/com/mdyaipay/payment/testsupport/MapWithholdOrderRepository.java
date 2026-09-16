package com.mdyaipay.payment.testsupport;

import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.domain.withhold.WithholdOrderRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MapWithholdOrderRepository implements WithholdOrderRepository {

    private final Map<String, WithholdOrder> store = new ConcurrentHashMap<>();

    @Override
    public WithholdOrder save(WithholdOrder order) {
        store.put(order.getDeductionNo(), order);
        return order;
    }

    @Override
    public Optional<WithholdOrder> findByDeductionNo(String deductionNo) {
        return Optional.ofNullable(store.get(deductionNo));
    }
}
