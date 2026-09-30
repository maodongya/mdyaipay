package com.mdyaipay.finance.repository.mybatis;

import com.mdyaipay.finance.domain.reconcile.BatchStatus;
import com.mdyaipay.finance.domain.reconcile.BillSource;
import com.mdyaipay.finance.domain.reconcile.DifferenceType;
import com.mdyaipay.finance.domain.reconcile.ReconciliationBatch;
import com.mdyaipay.finance.domain.reconcile.ReconciliationDifference;
import com.mdyaipay.finance.repository.ReconciliationBatchRepository;
import com.mdyaipay.finance.repository.mybatis.mapper.ReconciliationBatchMapper;
import com.mdyaipay.finance.repository.mybatis.row.ReconciliationBatchRow;
import com.mdyaipay.finance.repository.mybatis.row.ReconciliationDifferenceRow;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link ReconciliationBatchRepository} 的 MyBatis 实现。
 * <p>同一渠道、同一业务日覆盖差异行。不负责比对。</p>
 */
@Repository
public class MyBatisReconciliationBatchRepository implements ReconciliationBatchRepository {

    private final ReconciliationBatchMapper mapper;
    private final SnowflakeIdGenerator idGenerator;

    /**
     * @param mapper      对账 Mapper
     * @param idGenerator 批次与差异主键
     */
    public MyBatisReconciliationBatchRepository(ReconciliationBatchMapper mapper, SnowflakeIdGenerator idGenerator) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public Optional<ReconciliationBatch> find(String channel, LocalDate businessDate) {
        ReconciliationBatchRow row = mapper.findBatch(channel, businessDate.toString());
        if (row == null) {
            return Optional.empty();
        }
        List<ReconciliationDifference> differences = mapper.findDifferences(row.id()).stream()
                .map(MyBatisReconciliationBatchRepository::toDifference)
                .toList();
        return Optional.of(rehydrate(row, differences));
    }

    /** {@inheritDoc} 覆盖后按库中实际主键重写差异，避免重复键丢弃新主键。 */
    @Override
    public void save(ReconciliationBatch batch) {
        String day = batch.businessDate().toString();
        ReconciliationBatchRow existing = mapper.findBatch(batch.channel(), day);
        long proposedId = existing == null ? idGenerator.nextId() : existing.id();
        mapper.upsertBatch(new ReconciliationBatchRow(
                proposedId,
                batch.channel(),
                day,
                batch.status().name(),
                batch.billSource().name(),
                batch.matchedCount()));
        long batchId = mapper.findBatch(batch.channel(), day).id();
        mapper.deleteDifferences(batchId);
        for (ReconciliationDifference difference : batch.differences()) {
            mapper.insertDifference(toRow(idGenerator.nextId(), batchId, difference));
        }
    }

    private static ReconciliationBatch rehydrate(ReconciliationBatchRow row, List<ReconciliationDifference> differences) {
        return ReconciliationBatch.rehydrate(
                row.channel(),
                LocalDate.parse(row.businessDate()),
                BillSource.valueOf(row.billSource()),
                BatchStatus.valueOf(row.status()),
                row.matchedCount(),
                differences);
    }

    private static ReconciliationDifference toDifference(ReconciliationDifferenceRow row) {
        return new ReconciliationDifference(
                DifferenceType.valueOf(row.differenceType()),
                row.channelTradeNo(),
                row.orderNo(),
                row.localAmount(),
                row.channelAmount());
    }

    private static ReconciliationDifferenceRow toRow(long id, long batchId, ReconciliationDifference difference) {
        return new ReconciliationDifferenceRow(
                id,
                batchId,
                difference.type().name(),
                difference.channelTradeNo(),
                difference.orderNo(),
                difference.localAmount(),
                difference.channelAmount());
    }
}
