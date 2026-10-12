package com.mdyaipay.finance.api.dubbo;

import com.mdyaipay.finance.api.reconcile.ReconcileCommand;
import com.mdyaipay.finance.domain.reconcile.BillSource;
import com.mdyaipay.finance.gateway.ChannelBillGateway;
import com.mdyaipay.finance.gateway.CollectSuccessQuery;
import com.mdyaipay.finance.service.reconcile.ReconciliationApplicationService;
import com.mdyaipay.finance.testsupport.MapReconciliationBatchRepository;
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.logging.LogMessageCapture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 财务对账失败与差异事件日志。
 */
class ReconciliationFacadeAlertLoggingTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 30);

    /** 对账内部异常记录 reconciliation_failed。 */
    @Test
    void shouldLogReconciliationFailed() {
        CollectSuccessQuery query = (channel, day) -> {
            throw new IllegalStateException("payment query down");
        };
        ChannelBillGateway bills = (channel, day) -> List.of();
        ReconciliationFacadeImpl facade = new ReconciliationFacadeImpl(
                new ReconciliationApplicationService(new MapReconciliationBatchRepository(), query, bills));
        List<String> messages = LogMessageCapture.capture(ReconciliationFacadeImpl.class, () ->
                facade.reconcile(new ReconcileCommand("MOCK", DAY.toString(), BillSource.CHANNEL_PULL.name(), null)));
        assertTrue(messages.stream().anyMatch(line ->
                line.contains(AlertEvents.failure(AlertEvents.RECONCILIATION_FAILED))));
    }

    /** 存在差异记录 reconciliation_difference_found。 */
    @Test
    void shouldLogReconciliationDifferenceFound() {
        CollectSuccessQuery query = (channel, day) -> List.of(
                new com.mdyaipay.finance.domain.reconcile.LocalCollectSuccess("ORDER-1", "CH-1", 100L, "MOCK", DAY));
        ChannelBillGateway bills = (channel, day) -> List.of(
                new com.mdyaipay.finance.domain.reconcile.ChannelBillLine("CH-1", 80L, "MOCK", DAY));
        ReconciliationFacadeImpl facade = new ReconciliationFacadeImpl(
                new ReconciliationApplicationService(new MapReconciliationBatchRepository(), query, bills));
        List<String> messages = LogMessageCapture.capture(ReconciliationFacadeImpl.class, () ->
                facade.reconcile(new ReconcileCommand("MOCK", DAY.toString(), BillSource.CHANNEL_PULL.name(), null)));
        assertTrue(messages.stream().anyMatch(line ->
                line.contains(AlertEvents.failure(AlertEvents.RECONCILIATION_DIFFERENCE_FOUND))));
    }
}
