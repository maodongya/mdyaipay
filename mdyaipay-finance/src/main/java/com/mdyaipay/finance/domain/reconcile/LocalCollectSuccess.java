package com.mdyaipay.finance.domain.reconcile;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 参与对账的一笔收单成功单。金额单位为分。
 * <p>不负责查询支付库。</p>
 */
public record LocalCollectSuccess(
        String orderNo, String channelTradeNo, long amount, String channel, LocalDate businessDate) {

    /**
     * @param orderNo        我方收单单号，非空白
     * @param channelTradeNo 渠道交易号，非空白
     * @param amount         金额，单位分，必须大于 0
     * @param channel        渠道编码，非空白
     * @param businessDate   成功时的业务日，非空
     */
    public LocalCollectSuccess {
        orderNo = requireText(orderNo, "orderNo");
        channelTradeNo = requireText(channelTradeNo, "channelTradeNo");
        channel = requireText(channel, "channel");
        Objects.requireNonNull(businessDate, "businessDate must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
