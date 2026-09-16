package com.mdyaipay.payment.repository;

import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentOrderRepository;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import com.mdyaipay.payment.mybatis.mapper.PaymentOrderMapper;
import com.mdyaipay.payment.mybatis.row.PaymentOrderRow;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class MyBatisPaymentOrderRepository implements PaymentOrderRepository {

    private final PaymentOrderMapper paymentOrderMapper;

    public MyBatisPaymentOrderRepository(PaymentOrderMapper paymentOrderMapper) {
        this.paymentOrderMapper = Objects.requireNonNull(paymentOrderMapper, "paymentOrderMapper must not be null");
    }

    @Override
    public PaymentOrder save(PaymentOrder order) {
        paymentOrderMapper.upsert(toRow(order));
        return order;
    }

    @Override
    public Optional<PaymentOrder> findByOrderNo(String orderNo) {
        PaymentOrderRow row = paymentOrderMapper.findByOrderNo(orderNo);
        return row == null ? Optional.empty() : Optional.of(fromRow(row));
    }

    private static PaymentOrderRow toRow(PaymentOrder order) {
        return new PaymentOrderRow(
                order.getOrderNo(),
                order.getAmount(),
                order.getChannel(),
                order.getProductType().name(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private static PaymentOrder fromRow(PaymentOrderRow row) {
        return PaymentOrder.rehydrate(
                row.orderNo(),
                row.amount(),
                row.channel(),
                PaymentProductType.valueOf(row.productType()),
                PaymentStatus.valueOf(row.status()),
                row.createdAt(),
                row.updatedAt());
    }
}
