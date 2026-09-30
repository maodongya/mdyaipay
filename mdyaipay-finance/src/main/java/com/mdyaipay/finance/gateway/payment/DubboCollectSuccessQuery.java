package com.mdyaipay.finance.gateway.payment;

import com.mdyaipay.finance.domain.reconcile.LocalCollectSuccess;
import com.mdyaipay.finance.gateway.CollectSuccessQuery;
import com.mdyaipay.payment.api.gateway.PaymentGatewayFacade;
import com.mdyaipay.payment.api.gateway.dto.PaymentOrderView;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * {@link CollectSuccessQuery} 的 Dubbo 实现：调用支付 {@code listCollectSuccess}。
 * <p>不负责比对。</p>
 */
@Component
public class DubboCollectSuccessQuery implements CollectSuccessQuery {

    private final PaymentGatewayFacade paymentGatewayFacade;

    /**
     * @param paymentGatewayFacade 支付收单门面
     */
    public DubboCollectSuccessQuery(
            @DubboReference(version = "1.0.0", check = false, protocol = "tri")
            PaymentGatewayFacade paymentGatewayFacade) {
        this.paymentGatewayFacade = paymentGatewayFacade;
    }

    /** {@inheritDoc} */
    @Override
    public List<LocalCollectSuccess> list(String channel, LocalDate businessDate) {
        ApiResponse<List<PaymentOrderView>> response =
                paymentGatewayFacade.listCollectSuccess(channel, businessDate.toString());
        if (response.getCode() != ErrorCode.SUCCESS.getCode()) {
            throw new IllegalStateException(response.getMessage());
        }
        List<PaymentOrderView> data = response.getData();
        if (data == null) {
            return List.of();
        }
        return data.stream().map(view -> toLocal(view, businessDate)).toList();
    }

    private static LocalCollectSuccess toLocal(PaymentOrderView view, LocalDate businessDate) {
        return new LocalCollectSuccess(
                view.getOrderNo(),
                view.getChannelTradeNo(),
                view.getAmount(),
                view.getChannel(),
                businessDate);
    }
}
