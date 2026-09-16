package com.mdyaipay.payment.controller.dto;

public record PayoutRequest(String payoutNo, long amount, String channel, String payeeRef) {
}
