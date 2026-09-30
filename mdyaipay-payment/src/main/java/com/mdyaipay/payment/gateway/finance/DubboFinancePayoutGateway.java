package com.mdyaipay.payment.gateway.finance;

import com.mdyaipay.financegateway.api.FinanceChannelGatewayFacade;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutCommand;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutView;
import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.gateway.PayoutGateway;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 代付渠道端口：经 Dubbo 调 finance-gateway。
 */
@Component
@ConditionalOnProperty(name = "payment.channel.source", havingValue = "dubbo", matchIfMissing = true)
public class DubboFinancePayoutGateway implements PayoutGateway {

    @DubboReference(version = "1.0.0", check = false, protocol = "tri")
    private FinanceChannelGatewayFacade financeChannelGatewayFacade;

    /** {@inheritDoc} */
    @Override
    public boolean remit(PayoutOrder order) {
        ChannelPayoutCommand command =
                new ChannelPayoutCommand(order.getPayoutNo(), order.getAmount(), order.getChannel());
        ApiResponse<ChannelPayoutView> response = financeChannelGatewayFacade.remitPayout(command);
        return response.getCode() == ErrorCode.SUCCESS.getCode()
                && response.getData() != null
                && response.getData().isAccepted();
    }
}
