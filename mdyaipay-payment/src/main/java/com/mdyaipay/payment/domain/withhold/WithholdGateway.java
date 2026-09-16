package com.mdyaipay.payment.domain.withhold;

/**
 * 代扣渠道网关：单笔扣款、批量扣款等由具体适配器实现。
 */
public interface WithholdGateway {
    boolean deduct(WithholdOrder order);
}
