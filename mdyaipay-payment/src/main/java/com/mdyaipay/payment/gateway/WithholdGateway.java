package com.mdyaipay.payment.gateway;

import com.mdyaipay.payment.domain.withhold.WithholdOrder;

/**
 * 代扣渠道网关：单笔扣款、批量扣款等由具体适配器实现。
 * <p>
 * <b>不负责</b>：代扣单落库——见 {@link com.mdyaipay.payment.repository.WithholdOrderRepository}。
 */
public interface WithholdGateway {

    /**
     * 向渠道提交一笔代扣。
     * <p>
     * 本端口不落库；返回 {@code true} 表示渠道受理成功。
     */
    boolean deduct(WithholdOrder order);
}
