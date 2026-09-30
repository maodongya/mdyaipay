package com.mdyaipay.finance.testsupport;

import com.mdyaipay.finance.domain.reconcile.ReconciliationBatch;
import com.mdyaipay.finance.repository.ReconciliationBatchRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单测用进程内批次仓储。
 */
public final class MapReconciliationBatchRepository implements ReconciliationBatchRepository {

    private final ConcurrentHashMap<String, ReconciliationBatch> store = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public Optional<ReconciliationBatch> find(String channel, LocalDate businessDate) {
        return Optional.ofNullable(store.get(key(channel, businessDate)));
    }

    /** {@inheritDoc} */
    @Override
    public void save(ReconciliationBatch batch) {
        store.put(key(batch.channel(), batch.businessDate()), batch);
    }

    private static String key(String channel, LocalDate businessDate) {
        return channel + "|" + businessDate;
    }
}
