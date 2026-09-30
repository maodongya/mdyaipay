package com.mdyaipay.payment.gateway.finance;

import com.mdyaipay.financegateway.api.FinanceChannelGatewayFacade;
import com.mdyaipay.financegateway.api.collect.ChannelCollectCommand;
import com.mdyaipay.financegateway.api.collect.ChannelCollectSubmitResult;
import com.mdyaipay.financegateway.api.collect.ChannelCollectView;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;
import com.mdyaipay.payment.gateway.PaymentGateway;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 收单渠道端口：经 Dubbo 调 {@link FinanceChannelGatewayFacade}。
 */
@Component
@ConditionalOnProperty(name = "payment.channel.source", havingValue = "dubbo", matchIfMissing = true)
public class DubboFinancePaymentGateway implements PaymentGateway {

    @DubboReference(version = "1.0.0", check = false, protocol = "tri")
    private FinanceChannelGatewayFacade financeChannelGatewayFacade;

    /** {@inheritDoc} */
    @Override
    public PaymentSubmitResult pay(PaymentOrder order) {
        ChannelCollectCommand command = new ChannelCollectCommand(
                order.getOrderNo(),
                order.getAmount(),
                order.getChannel(),
                order.getProductType().name(),
                order.getMerchantId());
        ApiResponse<ChannelCollectView> response = financeChannelGatewayFacade.collect(command);
        if (response.getCode() != ErrorCode.SUCCESS.getCode() || response.getData() == null) {
            return PaymentSubmitResult.syncFailure();
        }
        ChannelCollectView view = response.getData();
        return switch (ChannelCollectSubmitResult.valueOf(view.getSubmitResult())) {
            case SYNC_SUCCESS -> PaymentSubmitResult.syncSuccess(view.getChannelTradeNo());
            case SYNC_FAILURE -> PaymentSubmitResult.syncFailure();
            case AWAITING_CHANNEL_CONFIRMATION -> PaymentSubmitResult.awaitingChannelConfirmation();
        };
    }
}
