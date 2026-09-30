package com.mdyaipay.financemock.service;

import com.mdyaipay.financegateway.api.collect.ChannelCollectCommand;
import com.mdyaipay.financegateway.api.collect.ChannelCollectSubmitResult;
import com.mdyaipay.financegateway.api.collect.ChannelCollectView;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutCommand;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutView;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdCommand;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdView;
import org.springframework.stereotype.Service;

/**
 * 模拟银行/第三方支付受理逻辑（由原 payment.gateway.mock 迁入）。
 */
@Service
public class ChannelMockService {

    /**
     * 模拟收单：网银待确认，其余按单号哈希约 80% 成功。
     */
    public ChannelCollectView collect(ChannelCollectCommand command) {
        if ("ONLINE_BANKING".equals(command.getProductType())) {
            return new ChannelCollectView(
                    ChannelCollectSubmitResult.AWAITING_CHANNEL_CONFIRMATION.name(), null);
        }
        boolean ok = Math.abs(command.getOrderNo().hashCode()) % 10 < 8;
        if (ok) {
            return new ChannelCollectView(
                    ChannelCollectSubmitResult.SYNC_SUCCESS.name(), "MOCK-" + command.getOrderNo());
        }
        return new ChannelCollectView(ChannelCollectSubmitResult.SYNC_FAILURE.name(), null);
    }

    /** 模拟代扣：按单号哈希约 80% 受理成功。 */
    public ChannelWithholdView deduct(ChannelWithholdCommand command) {
        boolean ok = Math.abs(command.getDeductionNo().hashCode()) % 10 < 8;
        return new ChannelWithholdView(ok);
    }

    /** 模拟代付：按单号哈希约 80% 受理成功。 */
    public ChannelPayoutView remit(ChannelPayoutCommand command) {
        boolean ok = Math.abs(command.getPayoutNo().hashCode()) % 10 < 8;
        return new ChannelPayoutView(ok);
    }
}
