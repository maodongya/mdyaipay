package com.mdyaipay.financegateway.api;

import com.mdyaipay.financegateway.api.collect.ChannelCollectCommand;
import com.mdyaipay.financegateway.api.collect.ChannelCollectView;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutCommand;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutView;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdCommand;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdView;
import com.mdyaipay.tools.model.ApiResponse;

/**
 * 渠道网关 Dubbo 门面：收单、代扣、代付统一出口。
 * <p>由 {@code mdyaipay-finance-gateway} 实现，经 HTTP 转发至银行/第三方或 finance-mock。</p>
 */
public interface FinanceChannelGatewayFacade {

    /** 提交收单；幂等由渠道与 orderNo 共同保证。 */
    ApiResponse<ChannelCollectView> collect(ChannelCollectCommand command);

    /** 提交代扣。 */
    ApiResponse<ChannelWithholdView> deductWithhold(ChannelWithholdCommand command);

    /** 提交代付。 */
    ApiResponse<ChannelPayoutView> remitPayout(ChannelPayoutCommand command);
}
