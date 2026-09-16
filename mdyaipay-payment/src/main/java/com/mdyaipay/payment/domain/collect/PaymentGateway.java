package com.mdyaipay.payment.domain.collect;

/**
 * 收单渠道网关：快捷、条码、网银跳转发起等「资金入账」类请求。
 * 网银通常为异步确认，适配器内负责登记渠道单号并等待回调驱动状态。
 */
public interface PaymentGateway {
    PaymentSubmitResult pay(PaymentOrder order);
}
