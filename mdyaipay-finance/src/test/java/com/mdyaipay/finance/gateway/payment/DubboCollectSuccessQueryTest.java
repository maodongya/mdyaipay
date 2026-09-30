package com.mdyaipay.finance.gateway.payment;

import com.mdyaipay.finance.domain.reconcile.LocalCollectSuccess;
import com.mdyaipay.payment.api.gateway.PaymentGatewayFacade;
import com.mdyaipay.payment.api.gateway.command.ChannelConfirmCommand;
import com.mdyaipay.payment.api.gateway.command.CollectPaymentCommand;
import com.mdyaipay.payment.api.gateway.command.CreatePayoutCommand;
import com.mdyaipay.payment.api.gateway.command.CreateWithholdCommand;
import com.mdyaipay.payment.api.gateway.dto.PaymentOrderView;
import com.mdyaipay.payment.api.gateway.dto.PayoutOrderView;
import com.mdyaipay.payment.api.gateway.dto.WithholdOrderView;
import com.mdyaipay.tools.model.ApiResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 支付成功单查询适配：把支付视图收成对账用的成功单。
 */
class DubboCollectSuccessQueryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 30);

    /** 支付返回成功单时映射渠道交易号与金额。 */
    @Test
    void shouldMapCollectSuccessViews() {
        PaymentOrderView view = new PaymentOrderView(
                "ORDER-1", 1L, 100L, "MOCK", "CH-1", "QUICK_COLLECTION", "SUCCESS", Instant.parse("2026-09-30T01:00:00Z"), Instant.parse("2026-09-30T02:00:00Z"));
        DubboCollectSuccessQuery query = new DubboCollectSuccessQuery(facade(ApiResponse.ok(List.of(view))));

        List<LocalCollectSuccess> lines = query.list("MOCK", DAY);

        assertEquals(1, lines.size());
        assertEquals("CH-1", lines.get(0).channelTradeNo());
        assertEquals(100L, lines.get(0).amount());
        assertEquals(DAY, lines.get(0).businessDate());
    }

    /** 支付返回失败码时拒绝对账读取。 */
    @Test
    void shouldRejectWhenPaymentReturnsError() {
        DubboCollectSuccessQuery query = new DubboCollectSuccessQuery(facade(ApiResponse.fail(10002, "bad channel")));
        assertThrows(IllegalStateException.class, () -> query.list("MOCK", DAY));
    }

    private static PaymentGatewayFacade facade(ApiResponse<List<PaymentOrderView>> listed) {
        return new PaymentGatewayFacade() {
            @Override
            public ApiResponse<PaymentOrderView> collect(CollectPaymentCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ApiResponse<PaymentOrderView> getPayment(String orderNo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ApiResponse<PaymentOrderView> confirmChannelPayment(ChannelConfirmCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ApiResponse<List<PaymentOrderView>> listCollectSuccess(String channel, String businessDate) {
                return listed;
            }

            @Override
            public ApiResponse<WithholdOrderView> createWithhold(CreateWithholdCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ApiResponse<PayoutOrderView> createPayout(CreatePayoutCommand command) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
