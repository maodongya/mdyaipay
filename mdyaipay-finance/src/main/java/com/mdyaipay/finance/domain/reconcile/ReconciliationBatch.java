package com.mdyaipay.finance.domain.reconcile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 某一渠道、某一业务日的对账批次。
 * <p>不负责读取账单或支付成功单。</p>
 */
public final class ReconciliationBatch {

    private final String channel;
    private final LocalDate businessDate;
    private final BillSource billSource;
    private final BatchStatus status;
    private final int matchedCount;
    private final List<ReconciliationDifference> differences;

    private ReconciliationBatch(
            String channel,
            LocalDate businessDate,
            BillSource billSource,
            BatchStatus status,
            int matchedCount,
            List<ReconciliationDifference> differences) {
        this.channel = channel;
        this.businessDate = businessDate;
        this.billSource = billSource;
        this.status = status;
        this.matchedCount = matchedCount;
        this.differences = List.copyOf(differences);
    }

    /**
     * 按渠道交易号比对。金额一致计平账，其余记差异。
     * <p>前置：两侧渠道、业务日必须与批次一致，且渠道交易号在各自一侧不重复。幂等：相同入参得到相同结果，不写库。</p>
     *
     * @param channel      批次渠道
     * @param businessDate 批次业务日
     * @param billSource   账单来源
     * @param localLines   我方成功单
     * @param billLines    渠道账单行
     */
    public static ReconciliationBatch reconcile(
            String channel,
            LocalDate businessDate,
            BillSource billSource,
            List<LocalCollectSuccess> localLines,
            List<ChannelBillLine> billLines) {
        String batchChannel = requireText(channel, "channel");
        Objects.requireNonNull(businessDate, "businessDate must not be null");
        Objects.requireNonNull(billSource, "billSource must not be null");
        Map<String, LocalCollectSuccess> local = indexLocal(batchChannel, businessDate, localLines);
        Map<String, ChannelBillLine> bills = indexBills(batchChannel, businessDate, billLines);
        List<ReconciliationDifference> differences = new ArrayList<>();
        int matched = collectDifferences(local, bills, differences);
        return new ReconciliationBatch(
                batchChannel, businessDate, billSource, BatchStatus.OPEN, matched, differences);
    }

    /**
     * 从持久化行重建批次，不再比对。
     * <p>前置：计数字段与差异行已由仓储读出。无副作用。</p>
     */
    public static ReconciliationBatch rehydrate(
            String channel,
            LocalDate businessDate,
            BillSource billSource,
            BatchStatus status,
            int matchedCount,
            List<ReconciliationDifference> differences) {
        return new ReconciliationBatch(
                requireText(channel, "channel"),
                Objects.requireNonNull(businessDate, "businessDate must not be null"),
                Objects.requireNonNull(billSource, "billSource must not be null"),
                Objects.requireNonNull(status, "status must not be null"),
                matchedCount,
                Objects.requireNonNull(differences, "differences must not be null"));
    }

    /** 平账笔数。 */
    public int matchedCount() {
        return matchedCount;
    }

    /** 差异行；平账不在其中。 */
    public List<ReconciliationDifference> differences() {
        return differences;
    }

    /** 批次状态。新建批次为 {@link BatchStatus#OPEN}。 */
    public BatchStatus status() {
        return status;
    }

    /** 批次渠道。 */
    public String channel() {
        return channel;
    }

    /** 批次业务日。 */
    public LocalDate businessDate() {
        return businessDate;
    }

    /** 账单来源。 */
    public BillSource billSource() {
        return billSource;
    }

    /**
     * 标记差异已处理。幂等：已是 {@link BatchStatus#PROCESSED} 时返回自身。
     * <p>本期没有对外处理接口，仅供拒绝重跑。</p>
     */
    public ReconciliationBatch markProcessed() {
        if (status == BatchStatus.PROCESSED) {
            return this;
        }
        return new ReconciliationBatch(channel, businessDate, billSource, BatchStatus.PROCESSED, matchedCount, differences);
    }

    private static Map<String, LocalCollectSuccess> indexLocal(
            String channel, LocalDate businessDate, List<LocalCollectSuccess> localLines) {
        Objects.requireNonNull(localLines, "localLines must not be null");
        Map<String, LocalCollectSuccess> indexed = new LinkedHashMap<>();
        for (LocalCollectSuccess line : localLines) {
            assertSameScope(channel, businessDate, line.channel(), line.businessDate());
            if (indexed.put(line.channelTradeNo(), line) != null) {
                throw new IllegalArgumentException("duplicate channelTradeNo: " + line.channelTradeNo());
            }
        }
        return indexed;
    }

    private static Map<String, ChannelBillLine> indexBills(
            String channel, LocalDate businessDate, List<ChannelBillLine> billLines) {
        Objects.requireNonNull(billLines, "billLines must not be null");
        Map<String, ChannelBillLine> indexed = new LinkedHashMap<>();
        for (ChannelBillLine line : billLines) {
            assertSameScope(channel, businessDate, line.channel(), line.businessDate());
            if (indexed.put(line.channelTradeNo(), line) != null) {
                throw new IllegalArgumentException("duplicate channelTradeNo: " + line.channelTradeNo());
            }
        }
        return indexed;
    }

    private static void assertSameScope(
            String batchChannel, LocalDate batchDate, String lineChannel, LocalDate lineDate) {
        if (!batchChannel.equals(lineChannel) || !batchDate.equals(lineDate)) {
            throw new IllegalArgumentException("line channel or businessDate does not match batch");
        }
    }

    private static int collectDifferences(
            Map<String, LocalCollectSuccess> local,
            Map<String, ChannelBillLine> bills,
            List<ReconciliationDifference> differences) {
        int matched = 0;
        for (LocalCollectSuccess line : local.values()) {
            ChannelBillLine bill = bills.remove(line.channelTradeNo());
            if (bill == null) {
                differences.add(localOnly(line));
            } else if (bill.amount() == line.amount()) {
                matched++;
            } else {
                differences.add(amountMismatch(line, bill));
            }
        }
        for (ChannelBillLine bill : bills.values()) {
            differences.add(channelOnly(bill));
        }
        return matched;
    }

    private static ReconciliationDifference localOnly(LocalCollectSuccess line) {
        return new ReconciliationDifference(
                DifferenceType.LOCAL_ONLY, line.channelTradeNo(), line.orderNo(), line.amount(), null);
    }

    private static ReconciliationDifference amountMismatch(LocalCollectSuccess line, ChannelBillLine bill) {
        return new ReconciliationDifference(
                DifferenceType.AMOUNT_MISMATCH,
                line.channelTradeNo(),
                line.orderNo(),
                line.amount(),
                bill.amount());
    }

    private static ReconciliationDifference channelOnly(ChannelBillLine bill) {
        return new ReconciliationDifference(
                DifferenceType.CHANNEL_ONLY, bill.channelTradeNo(), null, null, bill.amount());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
