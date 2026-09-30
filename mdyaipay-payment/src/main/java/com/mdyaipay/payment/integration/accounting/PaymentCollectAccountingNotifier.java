package com.mdyaipay.payment.integration.accounting;

import com.mdyaipay.payment.domain.collect.PaymentOrder;

/**
 * 收单成功后通知账务（Dubbo / MQ 等）；{@code channel=none} 时为 NOOP。
 */
public interface PaymentCollectAccountingNotifier {

    /** 空实现：不发送账务事件。 */
    PaymentCollectAccountingNotifier NOOP = order -> { };

    /**
     * 支付订单已进入 SUCCESS 时调用；实现方应异步或快速返回。
     * <p>幂等：同一 orderNo 可重复通知，账务侧按 bizKey 去重。</p>
     */
    void onCollectSuccess(PaymentOrder order);
}
