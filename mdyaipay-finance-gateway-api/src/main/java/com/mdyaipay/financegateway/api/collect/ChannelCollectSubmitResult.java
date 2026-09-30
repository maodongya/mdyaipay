package com.mdyaipay.financegateway.api.collect;

/**
 * 收单渠道受理结果（与 payment 域 {@code PaymentSubmitResult} 对齐）。
 */
public enum ChannelCollectSubmitResult {
    SYNC_SUCCESS,
    SYNC_FAILURE,
    AWAITING_CHANNEL_CONFIRMATION
}
