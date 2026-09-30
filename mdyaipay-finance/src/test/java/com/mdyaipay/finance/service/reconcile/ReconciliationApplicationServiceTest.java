package com.mdyaipay.finance.service.reconcile;

import com.mdyaipay.finance.domain.reconcile.BatchStatus;
import com.mdyaipay.finance.domain.reconcile.BillSource;
import com.mdyaipay.finance.domain.reconcile.ChannelBillLine;
import com.mdyaipay.finance.domain.reconcile.DifferenceType;
import com.mdyaipay.finance.domain.reconcile.LocalCollectSuccess;
import com.mdyaipay.finance.domain.reconcile.ReconciliationBatch;
import com.mdyaipay.finance.gateway.ChannelBillGateway;
import com.mdyaipay.finance.gateway.CollectSuccessQuery;
import com.mdyaipay.finance.repository.ReconciliationBatchRepository;
import com.mdyaipay.finance.testsupport.MapReconciliationBatchRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 对账应用服务：文件或渠道拉取账单，与支付成功单比对后落批次。
 */
class ReconciliationApplicationServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 30);

    /** 未处理批次重跑时用新结果覆盖差异。 */
    @Test
    void shouldReplaceOpenBatchWhenReconcileAgain() throws Exception {
        MapReconciliationBatchRepository repository = new MapReconciliationBatchRepository();
        ReconciliationApplicationService service = service(repository, List.of(local("CH-1", 100L)), List.of());
        Path mismatch = csv("CH-1", 80L);
        Path matched = csv("CH-1", 100L);

        ReconciliationBatch first = service.reconcile("MOCK", DAY, BillSource.FILE, mismatch);
        ReconciliationBatch second = service.reconcile("MOCK", DAY, BillSource.FILE, matched);

        assertEquals(DifferenceType.AMOUNT_MISMATCH, first.differences().get(0).type());
        assertEquals(1, second.matchedCount());
        assertEquals(0, second.differences().size());
        assertEquals(1, repository.find("MOCK", DAY).orElseThrow().matchedCount());
    }

    /** 已处理批次拒绝重跑。 */
    @Test
    void shouldRejectWhenBatchAlreadyProcessed() throws Exception {
        MapReconciliationBatchRepository repository = new MapReconciliationBatchRepository();
        ReconciliationBatch processed = ReconciliationBatch.reconcile(
                "MOCK", DAY, BillSource.FILE, List.of(local("CH-1", 100L)), List.of(bill("CH-1", 100L)))
                .markProcessed();
        repository.save(processed);
        ReconciliationApplicationService service = service(repository, List.of(), List.of());

        assertThrows(IllegalStateException.class, () ->
                service.reconcile("MOCK", DAY, BillSource.FILE, csv("CH-1", 100L)));
        assertEquals(BatchStatus.PROCESSED, repository.find("MOCK", DAY).orElseThrow().status());
    }

    /** 渠道拉取与文件导入走同一比对。 */
    @Test
    void shouldReconcileFromChannelPull() {
        MapReconciliationBatchRepository repository = new MapReconciliationBatchRepository();
        ReconciliationApplicationService service = service(
                repository,
                List.of(local("CH-1", 100L)),
                List.of(bill("CH-1", 100L)));

        ReconciliationBatch batch = service.reconcile("MOCK", DAY, BillSource.CHANNEL_PULL, null);

        assertEquals(BillSource.CHANNEL_PULL, batch.billSource());
        assertEquals(1, batch.matchedCount());
    }

    private static ReconciliationApplicationService service(
            ReconciliationBatchRepository repository,
            List<LocalCollectSuccess> local,
            List<ChannelBillLine> pulled) {
        CollectSuccessQuery query = (channel, day) -> local;
        ChannelBillGateway gateway = (channel, day) -> pulled;
        return new ReconciliationApplicationService(repository, query, gateway);
    }

    private static LocalCollectSuccess local(String tradeNo, long amount) {
        return new LocalCollectSuccess("ORDER-" + tradeNo, tradeNo, amount, "MOCK", DAY);
    }

    private static ChannelBillLine bill(String tradeNo, long amount) {
        return new ChannelBillLine(tradeNo, amount, "MOCK", DAY);
    }

    private static Path csv(String tradeNo, long amount) throws Exception {
        Path file = Files.createTempFile("channel-bill", ".csv");
        Files.writeString(file, """
                channelTradeNo,amount,channel,businessDate
                %s,%d,MOCK,%s
                """.formatted(tradeNo, amount, DAY));
        return file;
    }
}
