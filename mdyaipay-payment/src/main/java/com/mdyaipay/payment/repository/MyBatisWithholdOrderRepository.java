package com.mdyaipay.payment.repository;

import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.domain.withhold.WithholdOrderRepository;
import com.mdyaipay.payment.domain.withhold.WithholdStatus;
import com.mdyaipay.payment.mybatis.mapper.WithholdOrderMapper;
import com.mdyaipay.payment.mybatis.row.WithholdOrderRow;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class MyBatisWithholdOrderRepository implements WithholdOrderRepository {

    private final WithholdOrderMapper withholdOrderMapper;

    public MyBatisWithholdOrderRepository(WithholdOrderMapper withholdOrderMapper) {
        this.withholdOrderMapper = Objects.requireNonNull(withholdOrderMapper, "withholdOrderMapper must not be null");
    }

    @Override
    public WithholdOrder save(WithholdOrder order) {
        withholdOrderMapper.upsert(toRow(order));
        return order;
    }

    @Override
    public Optional<WithholdOrder> findByDeductionNo(String deductionNo) {
        WithholdOrderRow row = withholdOrderMapper.findByDeductionNo(deductionNo);
        return row == null ? Optional.empty() : Optional.of(fromRow(row));
    }

    private static WithholdOrderRow toRow(WithholdOrder order) {
        return new WithholdOrderRow(
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
