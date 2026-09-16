package com.mdyaipay.payment.controller;

import com.mdyaipay.payment.controller.dto.WithholdRequest;
import com.mdyaipay.payment.service.withhold.WithholdApplicationService;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/withholds")
public class WithholdRestController {

    private final WithholdApplicationService withholdService;

    public WithholdRestController(WithholdApplicationService withholdService) {
        this.withholdService = withholdService;
    }

    @PostMapping
    public WithholdOrder create(@RequestBody WithholdRequest body) {
        return withholdService.createAndDeduct(
                body.deductionNo(), body.agreementNo(), body.amount(), body.channel());
    }
}
