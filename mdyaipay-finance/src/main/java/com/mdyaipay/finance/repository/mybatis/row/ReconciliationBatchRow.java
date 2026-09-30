package com.mdyaipay.finance.repository.mybatis.row;

/**
 * 对账批次表行。{@code businessDate} 为 {@code yyyy-MM-dd}。
 */
public record ReconciliationBatchRow(
        long id,
        String channel,
        String businessDate,
        String status,
        String billSource,
        int matchedCount) {
}
