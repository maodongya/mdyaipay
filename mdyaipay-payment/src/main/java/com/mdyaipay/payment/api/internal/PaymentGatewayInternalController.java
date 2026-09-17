package com.mdyaipay.payment.api.internal;

import com.mdyaipay.payment.api.dubbo.PaymentGatewayFacadeImpl;
import com.mdyaipay.payment.api.gateway.command.CollectPaymentCommand;
import com.mdyaipay.payment.api.gateway.dto.PaymentOrderView;
import com.mdyaipay.tools.model.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内网 HTTP：供 gateway 调用（与 {@link PaymentGatewayFacadeImpl#collect} 等价）。
 * <p>不对外暴露；生产可关闭或网络隔离。</p>
 */
@RestController
@RequestMapping("/internal/v1/payment-gateway")
public class PaymentGatewayInternalController {

    private final PaymentGatewayFacadeImpl paymentGatewayFacade;

    public PaymentGatewayInternalController(PaymentGatewayFacadeImpl paymentGatewayFacade) {
        this.paymentGatewayFacade = paymentGatewayFacade;
    }

    /**
     * 收单，幂等语义与 Dubbo {@code collect} 一致（按 {@code orderNo}）。
     */
    @PostMapping("/collect")
    public ApiResponse<PaymentOrderView> collect(@RequestBody CollectPaymentCommand command) {
        return paymentGatewayFacade.collect(command);
    }
}
