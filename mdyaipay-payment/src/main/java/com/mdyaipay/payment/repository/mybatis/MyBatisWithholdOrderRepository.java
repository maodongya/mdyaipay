package com.mdyaipay.payment.repository.mybatis;

import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.repository.WithholdOrderRepository;
import com.mdyaipay.payment.domain.withhold.WithholdStatus;
import com.mdyaipay.payment.repository.mybatis.mapper.WithholdOrderMapper;
import com.mdyaipay.payment.repository.mybatis.row.WithholdOrderRow;
import com.mdyaipay.tools.id.UuidV7Generator;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * {@link WithholdOrderRepository} 的 MyBatis 实现。
 */
@Repository
public class MyBatisWithholdOrderRepository implements WithholdOrderRepository {

    private final WithholdOrderMapper withholdOrderMapper;
    private final UuidV7Generator uuidV7Generator;

    /**
     * @param withholdOrderMapper 代扣 Mapper
     * @param uuidV7Generator     订单主键 UUIDv7 生成器
     */
    public MyBatisWithholdOrderRepository(WithholdOrderMapper withholdOrderMapper, UuidV7Generator uuidV7Generator) {
        this.withholdOrderMapper = Objects.requireNonNull(withholdOrderMapper, "withholdOrderMapper must not be null");
        this.uuidV7Generator = Objects.requireNonNull(uuidV7Generator, "uuidV7Generator must not be null");
    }

    /** {@inheritDoc} 写入前按 deduction_no 解析或分配 UUIDv7 主键。 */
    @Override
    public WithholdOrder save(WithholdOrder order) {
        WithholdOrderRow existing = withholdOrderMapper.findByDeductionNo(order.getDeductionNo());
        byte[] rowId = MyBatisOrderUuidSupport.resolveRowId(existing == null ? null : existing.id(), uuidV7Generator);
        withholdOrderMapper.upsert(toRow(rowId, order));
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<WithholdOrder> findByDeductionNo(String deductionNo) {
        WithholdOrderRow row = withholdOrderMapper.findByDeductionNo(deductionNo);
        return row == null ? Optional.empty() : Optional.of(fromRow(row));
    }

    private static WithholdOrderRow toRow(byte[] id, WithholdOrder order) {
        return new WithholdOrderRow(
                id,
                order.getDeductionNo(),
                order.getAgreementNo(),
                order.getAmount(),
                order.getChannel(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private static WithholdOrder fromRow(WithholdOrderRow row) {
        return WithholdOrder.rehydrate(
                row.deductionNo(),
                row.agreementNo(),
                row.amount(),
                row.channel(),
                WithholdStatus.valueOf(row.status()),
                row.createdAt(),
                row.updatedAt());
    }
}
