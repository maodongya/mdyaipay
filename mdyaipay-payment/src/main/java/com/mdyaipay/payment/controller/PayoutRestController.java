package com.mdyaipay.payment.controller;

import com.mdyaipay.payment.controller.dto.PayoutRequest;
import com.mdyaipay.payment.service.payout.PayoutApplicationService;
import com.mdyaipay.payment.domain.payout.PayoutOrder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payouts")
public class PayoutRestController {

    private final PayoutApplicationService payoutService;

    public PayoutRestController(PayoutApplicationService payoutService) {
        this.payoutService = payoutService;
    }

    @PostMapping
    public PayoutOrder create(@RequestBody PayoutRequest body) {
        return payoutService.createAndRemit(body.payoutNo(), body.amount(), body.channel(), body.payeeRef());
    }
}
