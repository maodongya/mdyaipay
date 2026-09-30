package com.mdyaipay.financemock.service;

import com.mdyaipay.financegateway.api.collect.ChannelCollectCommand;
import com.mdyaipay.financegateway.api.collect.ChannelCollectSubmitResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 模拟渠道逻辑单测。 */
class ChannelMockServiceTest {

    /** 网银应返回待渠道确认。 */
    @Test
    void shouldAwaitConfirmationForOnlineBanking() {
        ChannelMockService service = new ChannelMockService();
        var view = service.collect(new ChannelCollectCommand(
                "OB-1", 100L, "MOCK", "ONLINE_BANKING", null));
        Assertions.assertEquals(
                ChannelCollectSubmitResult.AWAITING_CHANNEL_CONFIRMATION.name(), view.getSubmitResult());
        Assertions.assertNull(view.getChannelTradeNo());
    }
}
