package com.mdyaipay.payment.repository;

import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.domain.payout.PayoutOrderRepository;
import com.mdyaipay.payment.domain.payout.PayoutStatus;
import com.mdyaipay.payment.mybatis.mapper.PayoutOrderMapper;
import com.mdyaipay.payment.mybatis.row.PayoutOrderRow;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class MyBatisPayoutOrderRepository implements PayoutOrderRepository {

    private final PayoutOrderMapper payoutOrderMapper;

    public MyBatisPayoutOrderRepository(PayoutOrderMapper payoutOrderMapper) {
        this.payoutOrderMapper = Objects.requireNonNull(payoutOrderMapper, "payoutOrderMapper must not be null");
    }

    @Override
    public PayoutOrder save(PayoutOrder order) {
        payoutOrderMapper.upsert(toRow(order));
        return order;
    }

    @Override
    public Optional<PayoutOrder> findByPayoutNo(String payoutNo) {
        PayoutOrderRow row = payoutOrderMapper.findByPayoutNo(payoutNo);
        return row == null ? Optional.empty() : Optional.of(fromRow(row));
    }

    private static PayoutOrderRow toRow(PayoutOrder order) {
        return new PayoutOrderRow(
                order.getPayoutNo(),
                order.getAmount(),
                order.getChannel(),
                order.getPayeeRef(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private static PayoutOrder fromRow(PayoutOrderRow row) {
        return PayoutOrder.rehydrate(
                row.payoutNo(),
                row.amount(),
                row.channel(),
                row.payeeRef(),
                PayoutStatus.valueOf(row.status()),
                row.createdAt(),
                row.updatedAt());
    }
}
