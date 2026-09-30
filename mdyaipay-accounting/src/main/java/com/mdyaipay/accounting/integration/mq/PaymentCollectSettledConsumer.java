package com.mdyaipay.accounting.integration.mq;

import com.mdyaipay.accounting.api.mq.AccountingMqTopics;
import com.mdyaipay.accounting.api.mq.PaymentCollectSettledMessage;
import com.mdyaipay.accounting.service.wallet.MerchantWalletApplicationService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 收单成功 MQ 消费者：驱动商户待结算入账。
 */
@Component
@ConditionalOnProperty(name = "accounting.mq.enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = AccountingMqTopics.PAYMENT_COLLECT_SETTLED,
        consumerGroup = "${accounting.mq.payment-collect-consumer-group:mdyaipay-accounting-payment-collect}")
public class PaymentCollectSettledConsumer implements RocketMQListener<PaymentCollectSettledMessage> {

    private final MerchantWalletApplicationService merchantWalletApplicationService;

    public PaymentCollectSettledConsumer(MerchantWalletApplicationService merchantWalletApplicationService) {
        this.merchantWalletApplicationService = merchantWalletApplicationService;
    }

    /** 处理收单成功消息；幂等由应用层 bizKey 保证。 */
    @Override
    public void onMessage(PaymentCollectSettledMessage message) {
        merchantWalletApplicationService.onPaymentCollectSettled(message);
    }
}
