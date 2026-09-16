package com.mdyaipay.payment.controller;

import com.mdyaipay.payment.controller.dto.ChannelConfirmRequest;
import com.mdyaipay.payment.controller.dto.CollectPaymentRequest;
import com.mdyaipay.payment.service.collect.PaymentApplicationService;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentRestController {

    private final PaymentApplicationService paymentService;

    public PaymentRestController(PaymentApplicationService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/collect")
    public PaymentOrder collect(@RequestBody CollectPaymentRequest body) {
        PaymentProductType productType = body.productType() != null
                ? body.productType()
                : PaymentProductType.QUICK_COLLECTION;
        return paymentService.createAndPay(body.orderNo(), body.amount(), body.channel(), productType);
    }

    @GetMapping("/{orderNo}")
    public PaymentOrder get(@PathVariable String orderNo) {
        return paymentService.getPayment(orderNo);
    }

    @PostMapping("/{orderNo}/channel-confirm")
    public PaymentOrder channelConfirm(@PathVariable String orderNo, @RequestBody ChannelConfirmRequest body) {
        return paymentService.confirmChannelPayment(orderNo, body.success());
    }
}
