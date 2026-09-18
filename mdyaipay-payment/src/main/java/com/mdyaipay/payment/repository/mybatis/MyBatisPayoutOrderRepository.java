package com.mdyaipay.payment.repository.mybatis;

import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.repository.PayoutOrderRepository;
import com.mdyaipay.payment.domain.payout.PayoutStatus;
import com.mdyaipay.payment.repository.mybatis.mapper.PayoutOrderMapper;
import com.mdyaipay.payment.repository.mybatis.row.PayoutOrderRow;
import com.mdyaipay.tools.id.UuidV7Generator;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * {@link PayoutOrderRepository} 的 MyBatis 实现。
 */
@Repository
public class MyBatisPayoutOrderRepository implements PayoutOrderRepository {

    private final PayoutOrderMapper payoutOrderMapper;
    private final UuidV7Generator uuidV7Generator;

    /**
     * @param payoutOrderMapper 出款 Mapper
     * @param uuidV7Generator   订单主键 UUIDv7 生成器
     */
    public MyBatisPayoutOrderRepository(PayoutOrderMapper payoutOrderMapper, UuidV7Generator uuidV7Generator) {
        this.payoutOrderMapper = Objects.requireNonNull(payoutOrderMapper, "payoutOrderMapper must not be null");
        this.uuidV7Generator = Objects.requireNonNull(uuidV7Generator, "uuidV7Generator must not be null");
    }

    /** {@inheritDoc} 写入前按 payout_no 解析或分配 UUIDv7 主键。 */
    @Override
    public PayoutOrder save(PayoutOrder order) {
        PayoutOrderRow existing = payoutOrderMapper.findByPayoutNo(order.getPayoutNo());
        byte[] rowId = MyBatisOrderUuidSupport.resolveRowId(existing == null ? null : existing.id(), uuidV7Generator);
        payoutOrderMapper.upsert(toRow(rowId, order));
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<PayoutOrder> findByPayoutNo(String payoutNo) {
        PayoutOrderRow row = payoutOrderMapper.findByPayoutNo(payoutNo);
        return row == null ? Optional.empty() : Optional.of(fromRow(row));
    }

    private static PayoutOrderRow toRow(byte[] id, PayoutOrder order) {
        return new PayoutOrderRow(
                id,
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
