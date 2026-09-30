package com.mdyaipay.payment.integration.accounting;

import com.mdyaipay.accounting.api.mq.AccountingMqTopics;
import com.mdyaipay.accounting.api.mq.PaymentCollectSettledMessage;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

/**
 * 收单 SUCCESS 后向 RocketMQ 发送 {@link PaymentCollectSettledMessage}。
 */
@Component
@ConditionalOnProperty(name = "payment.accounting.channel", havingValue = "mq")
public class RocketPaymentCollectAccountingNotifier implements PaymentCollectAccountingNotifier {

    private final RocketMQTemplate rocketMqTemplate;

    public RocketPaymentCollectAccountingNotifier(RocketMQTemplate rocketMqTemplate) {
        this.rocketMqTemplate = Objects.requireNonNull(rocketMqTemplate);
    }

    /** {@inheritDoc} */
    @Override
    public void onCollectSuccess(PaymentOrder order) {
        if (order.getStatus() != PaymentStatus.SUCCESS) {
            return;
        }
        PaymentCollectSettledMessage message = new PaymentCollectSettledMessage(
                order.getOrderNo(),
                order.getAmount(),
                order.getMerchantId(),
                order.getChannel(),
                order.getProductType().name(),
                Instant.now());
        rocketMqTemplate.convertAndSend(AccountingMqTopics.PAYMENT_COLLECT_SETTLED, message);
    }
}
