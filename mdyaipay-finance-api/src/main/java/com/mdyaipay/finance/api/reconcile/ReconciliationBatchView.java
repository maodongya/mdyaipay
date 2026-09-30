package com.mdyaipay.finance.api.reconcile;

import java.io.Serializable;
import java.util.List;

/**
 * 对账批次视图。金额差异计数不含平账行。
 */
public final class ReconciliationBatchView implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String channel;
    private final String businessDate;
    private final String status;
    private final String billSource;
    private final int matchedCount;
    private final int amountMismatchCount;
    private final int localOnlyCount;
    private final int channelOnlyCount;
    private final List<ReconciliationDifferenceView> differences;

    /**
     * @param differences 差异行，不含平账
     */
    public ReconciliationBatchView(
            String channel,
            String businessDate,
            String status,
            String billSource,
            int matchedCount,
            int amountMismatchCount,
            int localOnlyCount,
            int channelOnlyCount,
            List<ReconciliationDifferenceView> differences) {
        this.channel = channel;
        this.businessDate = businessDate;
        this.status = status;
        this.billSource = billSource;
        this.matchedCount = matchedCount;
        this.amountMismatchCount = amountMismatchCount;
        this.localOnlyCount = localOnlyCount;
        this.channelOnlyCount = channelOnlyCount;
        this.differences = differences;
    }

    public String getChannel() {
        return channel;
    }

    public String getBusinessDate() {
        return businessDate;
    }

    public String getStatus() {
        return status;
    }

    public String getBillSource() {
        return billSource;
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public int getAmountMismatchCount() {
        return amountMismatchCount;
    }

    public int getLocalOnlyCount() {
        return localOnlyCount;
    }

    public int getChannelOnlyCount() {
        return channelOnlyCount;
    }

    public List<ReconciliationDifferenceView> getDifferences() {
        return differences;
    }
}
