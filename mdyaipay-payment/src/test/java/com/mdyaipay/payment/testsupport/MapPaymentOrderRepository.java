package com.mdyaipay.payment.testsupport;

import com.mdyaipay.payment.domain.collect.CollectBusinessDay;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import com.mdyaipay.payment.repository.PaymentOrderRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    /** {@inheritDoc} */
    @Override
    public List<PaymentOrder> findCollectSuccess(String channel, LocalDate businessDate) {
        Instant start = CollectBusinessDay.startInclusive(businessDate);
        Instant end = CollectBusinessDay.endExclusive(businessDate);
        return store.values().stream()
                .filter(order -> channel.equals(order.getChannel()))
                .filter(order -> order.getStatus() == PaymentStatus.SUCCESS)
                .filter(order -> order.getChannelTradeNo() != null && !order.getChannelTradeNo().isBlank())
                .filter(order -> !order.getUpdatedAt().isBefore(start) && order.getUpdatedAt().isBefore(end))
                .toList();
    }
}
