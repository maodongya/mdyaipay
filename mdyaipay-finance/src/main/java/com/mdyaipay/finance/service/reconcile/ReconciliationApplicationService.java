package com.mdyaipay.finance.service.reconcile;

import com.mdyaipay.finance.domain.reconcile.BatchStatus;
import com.mdyaipay.finance.domain.reconcile.BillSource;
import com.mdyaipay.finance.domain.reconcile.ChannelBillLine;
import com.mdyaipay.finance.domain.reconcile.ReconciliationBatch;
import com.mdyaipay.finance.gateway.ChannelBillGateway;
import com.mdyaipay.finance.gateway.CollectSuccessQuery;
import com.mdyaipay.finance.gateway.file.CsvChannelBillReader;
import com.mdyaipay.finance.repository.ReconciliationBatchRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 渠道对账编排：选择账单来源，与支付成功单比对后保存批次。
 * <p>不负责渠道报文解析与支付状态机。写路径在 READ COMMITTED 事务内执行。</p>
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ReconciliationApplicationService {

    private final ReconciliationBatchRepository repository;
    private final CollectSuccessQuery collectSuccessQuery;
    private final ChannelBillGateway channelBillGateway;

    /**
     * @param repository           批次仓储
     * @param collectSuccessQuery  支付成功单查询
     * @param channelBillGateway   渠道账单拉取
     */
    public ReconciliationApplicationService(
            ReconciliationBatchRepository repository,
            CollectSuccessQuery collectSuccessQuery,
            ChannelBillGateway channelBillGateway) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.collectSuccessQuery = Objects.requireNonNull(collectSuccessQuery, "collectSuccessQuery must not be null");
        this.channelBillGateway = Objects.requireNonNull(channelBillGateway, "channelBillGateway must not be null");
    }

    /**
     * 对某一渠道、某一业务日执行对账。
     * <p>前置：{@link BillSource#FILE} 时 {@code file} 非空。已处理批次拒绝重跑。
     * 幂等：未处理批次再次执行覆盖差异，不保留旧差异。</p>
     */
    public ReconciliationBatch reconcile(String channel, LocalDate businessDate, BillSource source, Path file) {
        Objects.requireNonNull(businessDate, "businessDate must not be null");
        Objects.requireNonNull(source, "source must not be null");
        rejectIfProcessed(channel, businessDate);
        /* 功能块：装入两侧明细 — 文件与拉取都变成账单行，再和支付成功单比对 */
        List<ChannelBillLine> bills = loadBills(channel, businessDate, source, file);
        ReconciliationBatch batch = ReconciliationBatch.reconcile(
                channel,
                businessDate,
                source,
                collectSuccessQuery.list(channel, businessDate),
                bills);
        repository.save(batch);
        return batch;
    }

    /**
     * 查询已保存的批次。无则 empty。无副作用。
     */
    public java.util.Optional<ReconciliationBatch> find(String channel, LocalDate businessDate) {
        return repository.find(channel, businessDate);
    }

    private void rejectIfProcessed(String channel, LocalDate businessDate) {
        repository.find(channel, businessDate).ifPresent(existing -> {
            if (existing.status() == BatchStatus.PROCESSED) {
                throw new IllegalStateException("batch already processed");
            }
        });
    }

    private List<ChannelBillLine> loadBills(String channel, LocalDate businessDate, BillSource source, Path file) {
        if (source == BillSource.FILE) {
            if (file == null) {
                throw new IllegalArgumentException("file must not be null");
            }
            return CsvChannelBillReader.read(file);
        }
        return channelBillGateway.pull(channel, businessDate);
    }
}
