package com.mdyaipay.finance.domain.reconcile;

import java.util.Objects;

/**
 * 一条对账差异。平账不产生本对象。
 */
public record ReconciliationDifference(
        DifferenceType type,
        String channelTradeNo,
        String orderNo,
        Long localAmount,
        Long channelAmount) {

    /**
     * @param type           差异类型
     * @param channelTradeNo 渠道交易号，非空白
     * @param orderNo        我方单号；渠道单边可空
     * @param localAmount    我方金额（分）；渠道单边可空
     * @param channelAmount  渠道金额（分）；我方单边可空
     */
    public ReconciliationDifference {
        Objects.requireNonNull(type, "type must not be null");
        if (channelTradeNo == null || channelTradeNo.isBlank()) {
            throw new IllegalArgumentException("channelTradeNo must not be blank");
        }
    }
}
