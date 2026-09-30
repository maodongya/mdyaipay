package com.mdyaipay.financemock.api;

import com.mdyaipay.financegateway.api.collect.ChannelCollectCommand;
import com.mdyaipay.financegateway.api.collect.ChannelCollectView;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutCommand;
import com.mdyaipay.financegateway.api.payout.ChannelPayoutView;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdCommand;
import com.mdyaipay.financegateway.api.withhold.ChannelWithholdView;
import com.mdyaipay.financemock.service.ChannelMockService;
import com.mdyaipay.tools.model.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内网 HTTP：供 finance-gateway 调用的模拟渠道接口。
 */
@RestController
@RequestMapping("/internal/channel")
public class ChannelMockController {

    private final ChannelMockService channelMockService;

    public ChannelMockController(ChannelMockService channelMockService) {
        this.channelMockService = channelMockService;
    }

    /** 收单模拟。 */
    @PostMapping("/collect")
    public ApiResponse<ChannelCollectView> collect(@RequestBody ChannelCollectCommand command) {
        return ApiResponse.ok(channelMockService.collect(command));
    }

    /** 代扣模拟。 */
    @PostMapping("/withhold/deduct")
    public ApiResponse<ChannelWithholdView> deduct(@RequestBody ChannelWithholdCommand command) {
        return ApiResponse.ok(channelMockService.deduct(command));
    }

    /** 代付模拟。 */
    @PostMapping("/payout/remit")
    public ApiResponse<ChannelPayoutView> remit(@RequestBody ChannelPayoutCommand command) {
        return ApiResponse.ok(channelMockService.remit(command));
    }
}
