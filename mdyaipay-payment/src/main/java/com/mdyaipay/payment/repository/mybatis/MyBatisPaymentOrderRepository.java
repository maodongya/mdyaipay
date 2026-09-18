package com.mdyaipay.payment.repository.mybatis;

import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.repository.PaymentOrderRepository;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import com.mdyaipay.payment.repository.mybatis.mapper.PaymentOrderMapper;
import com.mdyaipay.payment.repository.mybatis.row.PaymentOrderRow;
import com.mdyaipay.tools.id.UuidV7Generator;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * {@link PaymentOrderRepository} 的 MyBatis 实现。
 */
@Repository
public class MyBatisPaymentOrderRepository implements PaymentOrderRepository {

    private final PaymentOrderMapper paymentOrderMapper;
    private final UuidV7Generator uuidV7Generator;

    /**
     * @param paymentOrderMapper 收单 Mapper
     * @param uuidV7Generator    订单主键 UUIDv7 生成器
     */
    public MyBatisPaymentOrderRepository(PaymentOrderMapper paymentOrderMapper, UuidV7Generator uuidV7Generator) {
        this.paymentOrderMapper = Objects.requireNonNull(paymentOrderMapper, "paymentOrderMapper must not be null");
        this.uuidV7Generator = Objects.requireNonNull(uuidV7Generator, "uuidV7Generator must not be null");
    }

    /** {@inheritDoc} 写入前按 order_no 解析或分配 UUIDv7 主键。 */
    @Override
    public PaymentOrder save(PaymentOrder order) {
        PaymentOrderRow existing = paymentOrderMapper.findByOrderNo(order.getOrderNo());
        byte[] rowId = MyBatisOrderUuidSupport.resolveRowId(existing == null ? null : existing.id(), uuidV7Generator);
        paymentOrderMapper.upsert(toRow(rowId, order));
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<PaymentOrder> findByOrderNo(String orderNo) {
        PaymentOrderRow row = paymentOrderMapper.findByOrderNo(orderNo);
        return row == null ? Optional.empty() : Optional.of(fromRow(row));
    }

    private static PaymentOrderRow toRow(byte[] id, PaymentOrder order) {
        return new PaymentOrderRow(
                id,
                order.getOrderNo(),
                order.getMerchantId(),
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
                row.merchantId(),
                PaymentStatus.valueOf(row.status()),
                row.createdAt(),
                row.updatedAt());
    }
}
