package com.mdyaipay.payment.config;

import com.mdyaipay.payment.gateway.PaymentGateway;
import com.mdyaipay.payment.gateway.PayoutGateway;
import com.mdyaipay.payment.gateway.WithholdGateway;
import com.mdyaipay.payment.gateway.mock.MockPaymentGateway;
import com.mdyaipay.payment.gateway.mock.MockPayoutGateway;
import com.mdyaipay.payment.gateway.mock.MockWithholdGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 进程内 Mock 渠道（单测/无 finance-gateway 时使用）。
 */
@Configuration
@ConditionalOnProperty(name = "payment.channel.source", havingValue = "in-process-mock")
public class InProcessMockChannelConfiguration {

    @Bean
    PaymentGateway paymentGateway() {
        return new MockPaymentGateway();
    }

    @Bean
    WithholdGateway withholdGateway() {
        return new MockWithholdGateway();
    }

    @Bean
    PayoutGateway payoutGateway() {
        return new MockPayoutGateway();
    }
}
