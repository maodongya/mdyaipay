package com.mdyaipay.payment.gateway;

import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;

/**
 * 收单渠道网关：快捷、条码、网银跳转发起等「资金入账」类请求。
 * 网银通常为异步确认，适配器内负责登记渠道单号并等待回调驱动状态。
 * <p>
 * <b>不负责</b>：订单落库与状态机——见 {@link com.mdyaipay.payment.repository.PaymentOrderRepository}、
 * {@link PaymentOrder}。
 */
public interface PaymentGateway {

    /**
     * 向渠道提交收单。
     * <p>
     * 本端口不落库；幂等由渠道与调用方业务单号共同保证。
     */
    PaymentSubmitResult pay(PaymentOrder order);
}
