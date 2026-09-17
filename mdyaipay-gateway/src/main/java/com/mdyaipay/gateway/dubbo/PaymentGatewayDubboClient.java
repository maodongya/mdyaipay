package com.mdyaipay.gateway.dubbo;

import com.mdyaipay.payment.api.gateway.PaymentGatewayFacade;
import com.mdyaipay.payment.api.gateway.command.ChannelConfirmCommand;
import com.mdyaipay.payment.api.gateway.command.CollectPaymentCommand;
import com.mdyaipay.payment.api.gateway.command.CreatePayoutCommand;
import com.mdyaipay.payment.api.gateway.command.CreateWithholdCommand;
import com.mdyaipay.payment.api.gateway.dto.PaymentOrderView;
import com.mdyaipay.payment.api.gateway.dto.PayoutOrderView;
import com.mdyaipay.payment.api.gateway.dto.WithholdOrderView;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 网关 → payment Dubbo 客户端（ZK 注册发现）。
 * <p>薄封装 {@link PaymentGatewayFacade}，便于 Filter 注入与单测替换。</p>
 */
@Component
public class PaymentGatewayDubboClient {

    @DubboReference(version = "1.0.0", check = false)
    private PaymentGatewayFacade paymentGatewayFacade;

    /** {@link PaymentGatewayFacade#collect} */
    public ApiResponse<PaymentOrderView> collect(CollectPaymentCommand command) {
        return paymentGatewayFacade.collect(command);
    }

    /** {@link PaymentGatewayFacade#getPayment} */
    public ApiResponse<PaymentOrderView> getPayment(String orderNo) {
        return paymentGatewayFacade.getPayment(orderNo);
    }

    /** {@link PaymentGatewayFacade#confirmChannelPayment} */
    public ApiResponse<PaymentOrderView> confirmChannelPayment(ChannelConfirmCommand command) {
        return paymentGatewayFacade.confirmChannelPayment(command);
    }

    /** {@link PaymentGatewayFacade#createWithhold} */
    public ApiResponse<WithholdOrderView> createWithhold(CreateWithholdCommand command) {
        return paymentGatewayFacade.createWithhold(command);
    }

    /** {@link PaymentGatewayFacade#createPayout} */
    public ApiResponse<PayoutOrderView> createPayout(CreatePayoutCommand command) {
        return paymentGatewayFacade.createPayout(command);
    }
}
