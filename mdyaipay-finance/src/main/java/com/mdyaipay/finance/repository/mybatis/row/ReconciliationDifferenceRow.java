package com.mdyaipay.finance.repository.mybatis.row;

/**
 * 对账差异表行。金额单位为分，单边缺失时为 null。
 */
public record ReconciliationDifferenceRow(
        long id,
        long batchId,
        String differenceType,
        String channelTradeNo,
        String orderNo,
        Long localAmount,
        Long channelAmount) {
}
