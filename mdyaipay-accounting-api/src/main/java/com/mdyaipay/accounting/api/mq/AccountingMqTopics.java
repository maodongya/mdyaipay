package com.mdyaipay.accounting.api.mq;

/**
 * 账务域相关 RocketMQ Topic 常量。
 */
public final class AccountingMqTopics {

    /** 收单支付成功后的入账驱动事件（payment 生产，accounting 消费）。 */
    public static final String PAYMENT_COLLECT_SETTLED = "MDYAIPAY_PAYMENT_COLLECT_SETTLED";

    private AccountingMqTopics() {
    }
}
