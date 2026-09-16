package com.mdyaipay.payment.domain.collect;

/**
 * 收单渠道受理结果：同步终态或等待渠道异步确认（如网银）。
 */
public enum PaymentSubmitResult {
    SYNC_SUCCESS,
    SYNC_FAILURE,
    AWAITING_CHANNEL_CONFIRMATION
}
