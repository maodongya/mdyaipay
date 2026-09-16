package com.mdyaipay.payment.controller.dto;

import com.mdyaipay.payment.domain.collect.PaymentProductType;

public record CollectPaymentRequest(
        String orderNo,
        long amount,
        String channel,
        PaymentProductType productType
) {
}
