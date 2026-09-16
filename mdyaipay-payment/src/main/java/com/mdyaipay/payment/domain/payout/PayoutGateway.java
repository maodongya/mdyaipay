package com.mdyaipay.payment.domain.payout;

/**
 * 代付渠道网关：单笔打款、批量代付等由具体适配器实现。
 */
public interface PayoutGateway {
    boolean remit(PayoutOrder order);
}
