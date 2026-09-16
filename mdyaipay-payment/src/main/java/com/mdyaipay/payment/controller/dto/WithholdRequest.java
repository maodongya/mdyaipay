package com.mdyaipay.payment.controller.dto;

public record WithholdRequest(String deductionNo, String agreementNo, long amount, String channel) {
}
