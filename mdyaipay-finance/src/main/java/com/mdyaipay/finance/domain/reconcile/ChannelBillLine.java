package com.mdyaipay.finance.domain.reconcile;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 渠道账单中的一行。金额单位为分。
 * <p>不负责文件解析。</p>
 */
public record ChannelBillLine(String channelTradeNo, long amount, String channel, LocalDate businessDate) {

    /**
     * @param channelTradeNo 渠道交易号，非空白
     * @param amount         金额，单位分，必须大于 0
     * @param channel        渠道编码，非空白
     * @param businessDate   业务日，非空
     */
    public ChannelBillLine {
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
