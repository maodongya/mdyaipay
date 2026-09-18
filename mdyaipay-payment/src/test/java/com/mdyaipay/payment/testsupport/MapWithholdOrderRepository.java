package com.mdyaipay.payment.testsupport;

import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.repository.WithholdOrderRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 单测用进程内 Map 仓储，非生产实现。 */
public final class MapWithholdOrderRepository implements WithholdOrderRepository {

    private final Map<String, WithholdOrder> store = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public WithholdOrder save(WithholdOrder order) {
        store.put(order.getDeductionNo(), order);
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<WithholdOrder> findByDeductionNo(String deductionNo) {
        return Optional.ofNullable(store.get(deductionNo));
    }
}
