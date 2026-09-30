package com.mdyaipay.finance.domain.reconcile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渠道对账批次：按渠道交易号比对收单成功单与账单行。
 */
class ReconciliationBatchTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 30);

    /** 渠道交易号与金额一致时记平账，不产生差异。 */
    @Test
    void shouldMatchWhenChannelTradeNoAndAmountEqual() {
        ReconciliationBatch batch = ReconciliationBatch.reconcile(
                "MOCK",
                DAY,
                BillSource.FILE,
                List.of(new LocalCollectSuccess("ORDER-1", "CH-1", 100L, "MOCK", DAY)),
                List.of(new ChannelBillLine("CH-1", 100L, "MOCK", DAY)));

        assertEquals(1, batch.matchedCount());
        assertTrue(batch.differences().isEmpty());
        assertEquals(BatchStatus.OPEN, batch.status());
    }

    /** 渠道交易号相同但金额不同时记金额不符。 */
    @Test
    void shouldRecordAmountMismatchWhenAmountsDiffer() {
        ReconciliationBatch batch = ReconciliationBatch.reconcile(
                "MOCK",
                DAY,
                BillSource.CHANNEL_PULL,
                List.of(new LocalCollectSuccess("ORDER-1", "CH-1", 100L, "MOCK", DAY)),
                List.of(new ChannelBillLine("CH-1", 80L, "MOCK", DAY)));

        assertEquals(0, batch.matchedCount());
        assertEquals(1, batch.differences().size());
        ReconciliationDifference diff = batch.differences().get(0);
        assertEquals(DifferenceType.AMOUNT_MISMATCH, diff.type());
        assertEquals("ORDER-1", diff.orderNo());
        assertEquals(100L, diff.localAmount());
        assertEquals(80L, diff.channelAmount());
    }

    /** 我方有、渠道无时记单边。 */
    @Test
    void shouldRecordLocalOnlyWhenBillMissesTrade() {
        ReconciliationBatch batch = ReconciliationBatch.reconcile(
                "MOCK",
                DAY,
                BillSource.FILE,
                List.of(new LocalCollectSuccess("ORDER-1", "CH-1", 100L, "MOCK", DAY)),
                List.of());

        assertEquals(DifferenceType.LOCAL_ONLY, batch.differences().get(0).type());
        assertEquals("CH-1", batch.differences().get(0).channelTradeNo());
        assertEquals(null, batch.differences().get(0).channelAmount());
    }

    /** 渠道有、我方无时记单边。 */
    @Test
    void shouldRecordChannelOnlyWhenLocalMissesTrade() {
        ReconciliationBatch batch = ReconciliationBatch.reconcile(
                "MOCK",
                DAY,
                BillSource.FILE,
                List.of(),
                List.of(new ChannelBillLine("CH-9", 50L, "MOCK", DAY)));

        assertEquals(DifferenceType.CHANNEL_ONLY, batch.differences().get(0).type());
        assertEquals(null, batch.differences().get(0).orderNo());
        assertEquals(50L, batch.differences().get(0).channelAmount());
    }

    /** 任一侧渠道交易号重复则拒绝整批。 */
    @Test
    void shouldRejectDuplicateChannelTradeNo() {
        List<LocalCollectSuccess> local = List.of(
                new LocalCollectSuccess("ORDER-1", "CH-1", 100L, "MOCK", DAY),
                new LocalCollectSuccess("ORDER-2", "CH-1", 100L, "MOCK", DAY));
        assertThrows(IllegalArgumentException.class, () -> ReconciliationBatch.reconcile(
                "MOCK", DAY, BillSource.FILE, local, List.of()));
    }

    /** 账单渠道或业务日与批次不一致时拒绝。 */
    @Test
    void shouldRejectBillLineOutsideBatch() {
        List<ChannelBillLine> otherChannel = List.of(new ChannelBillLine("CH-1", 100L, "OTHER", DAY));
        assertThrows(IllegalArgumentException.class, () -> ReconciliationBatch.reconcile(
                "MOCK", DAY, BillSource.FILE, List.of(), otherChannel));

        List<ChannelBillLine> otherDay = List.of(
                new ChannelBillLine("CH-1", 100L, "MOCK", DAY.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> ReconciliationBatch.reconcile(
                "MOCK", DAY, BillSource.FILE, List.of(), otherDay));
    }
}
