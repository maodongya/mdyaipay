package com.mdyaipay.payment.integration.accounting;

import com.mdyaipay.accounting.api.collect.CollectAccountingFacade;
import com.mdyaipay.accounting.api.collect.PaymentCollectSuccessCommand;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 收单 SUCCESS 后通过 Dubbo 调用 {@link CollectAccountingFacade} 入账。
 */
@Component
@ConditionalOnProperty(name = "payment.accounting.channel", havingValue = "dubbo", matchIfMissing = true)
public class DubboPaymentCollectAccountingNotifier implements PaymentCollectAccountingNotifier {

    private static final Logger log = LogManager.getLogger(DubboPaymentCollectAccountingNotifier.class);

    @DubboReference(version = "1.0.0", check = false, protocol = "tri")
    private CollectAccountingFacade collectAccountingFacade;

    /** {@inheritDoc} */
    @Override
    public void onCollectSuccess(PaymentOrder order) {
        if (order.getStatus() != PaymentStatus.SUCCESS) {
            return;
        }
        PaymentCollectSuccessCommand command = new PaymentCollectSuccessCommand(
                order.getOrderNo(),
                order.getAmount(),
                order.getMerchantId(),
                order.getChannel(),
                order.getProductType().name(),
                order.getUpdatedAt() != null ? order.getUpdatedAt() : Instant.now());
        ApiResponse<Void> response = collectAccountingFacade.onPaymentCollectSuccess(command);
        if (response.getCode() != ErrorCode.SUCCESS.getCode()) {
            log.warn(
                    "event=payment_collect_accounting_dubbo_failed orderNo={} code={} message={}",
                    order.getOrderNo(),
                    response.getCode(),
                    response.getMessage());
        }
    }
}
