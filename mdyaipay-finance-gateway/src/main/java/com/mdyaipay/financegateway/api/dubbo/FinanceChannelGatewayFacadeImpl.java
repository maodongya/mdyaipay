package com.mdyaipay.financegateway.api.dubbo;

import com.mdyaipay.financegateway.api.FinanceChannelGatewayFacade;
import com.mdyaipay.financegateway.api.collect.ChannelCollectCommand;
import com.mdyaipay.financegateway.api.collect.ChannelCollectView;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutCommand;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutView;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdCommand;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdView;
import com.mdyaipay.financegateway.integration.FinanceChannelHttpClient;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * 渠道网关 Dubbo Provider：薄转发至 HTTP 下游。
 */
@Component
@DubboService(version = "1.0.0")
public class FinanceChannelGatewayFacadeImpl implements FinanceChannelGatewayFacade {

    private final FinanceChannelHttpClient financeChannelHttpClient;

    public FinanceChannelGatewayFacadeImpl(FinanceChannelHttpClient financeChannelHttpClient) {
        this.financeChannelHttpClient = financeChannelHttpClient;
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<ChannelCollectView> collect(ChannelCollectCommand command) {
        return financeChannelHttpClient.collect(command);
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<ChannelWithholdView> deductWithhold(ChannelWithholdCommand command) {
        return financeChannelHttpClient.deduct(command);
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<ChannelPayoutView> remitPayout(ChannelPayoutCommand command) {
        return financeChannelHttpClient.remit(command);
    }
}
