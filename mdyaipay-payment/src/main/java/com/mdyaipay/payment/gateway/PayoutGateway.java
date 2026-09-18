package com.mdyaipay.payment.gateway;

import com.mdyaipay.payment.domain.payout.PayoutOrder;

/**
 * 代付渠道网关：单笔打款、批量代付等由具体适配器实现。
 * <p>
 * <b>不负责</b>：出款单落库——见 {@link com.mdyaipay.payment.repository.PayoutOrderRepository}。
 */
public interface PayoutGateway {

    /**
     * 向渠道提交一笔代付。
     * <p>
     * 本端口不落库；返回 {@code true} 表示渠道受理成功。
     */
    boolean remit(PayoutOrder order);
}
