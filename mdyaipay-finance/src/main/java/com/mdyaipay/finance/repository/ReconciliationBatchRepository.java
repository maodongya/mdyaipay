package com.mdyaipay.finance.repository;

import com.mdyaipay.finance.domain.reconcile.ReconciliationBatch;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 对账批次持久化端口。
 * <p>不负责比对规则。</p>
 */
public interface ReconciliationBatchRepository {

    /**
     * 按渠道与业务日查询批次。无行时 empty。无副作用。
     */
    Optional<ReconciliationBatch> find(String channel, LocalDate businessDate);

    /**
     * 保存批次及其差异。同一渠道、同一业务日再次保存会覆盖。
     * <p>幂等：相同批次重复保存不产生第二行。</p>
     */
    void save(ReconciliationBatch batch);
}
