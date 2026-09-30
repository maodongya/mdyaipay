package com.mdyaipay.payment.config;

import com.mdyaipay.payment.integration.accounting.PaymentCollectAccountingNotifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 账务通知 Bean：{@code payment.accounting.channel=none} 时使用 NOOP。
 */
@Configuration
@EnableConfigurationProperties(PaymentAccountingProperties.class)
public class PaymentAccountingConfiguration {

    @Bean
    @ConditionalOnProperty(name = "payment.accounting.channel", havingValue = "none")
    PaymentCollectAccountingNotifier noopPaymentCollectAccountingNotifier() {
        return PaymentCollectAccountingNotifier.NOOP;
    }
}
