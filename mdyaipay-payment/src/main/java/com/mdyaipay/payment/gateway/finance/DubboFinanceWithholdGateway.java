package com.mdyaipay.payment.gateway.finance;

import com.mdyaipay.financegateway.api.FinanceChannelGatewayFacade;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdCommand;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdView;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.gateway.WithholdGateway;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 代扣渠道端口：经 Dubbo 调 finance-gateway。
 */
@Component
@ConditionalOnProperty(name = "payment.channel.source", havingValue = "dubbo", matchIfMissing = true)
public class DubboFinanceWithholdGateway implements WithholdGateway {

    @DubboReference(version = "1.0.0", check = false, protocol = "tri")
    private FinanceChannelGatewayFacade financeChannelGatewayFacade;

    /** {@inheritDoc} */
    @Override
    public boolean deduct(WithholdOrder order) {
        ChannelWithholdCommand command = new ChannelWithholdCommand(
                order.getDeductionNo(),
                order.getAmount(),
                order.getChannel(),
                order.getAgreementNo());
        ApiResponse<ChannelWithholdView> response = financeChannelGatewayFacade.deductWithhold(command);
        return response.getCode() == ErrorCode.SUCCESS.getCode()
                && response.getData() != null
                && response.getData().isAccepted();
    }
}
