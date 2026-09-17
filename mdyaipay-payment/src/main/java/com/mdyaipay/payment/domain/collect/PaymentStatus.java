package com.mdyaipay.payment.domain.collect;

/**
 * 收单生命周期状态。
 */
public enum PaymentStatus {
    CREATED,
    PROCESSING,
    SUCCESS,
    FAILED,
    CLOSED
}
