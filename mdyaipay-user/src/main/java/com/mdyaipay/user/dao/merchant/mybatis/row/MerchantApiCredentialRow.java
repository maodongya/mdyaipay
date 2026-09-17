package com.mdyaipay.user.dao.merchant.mybatis.row;

import java.time.Instant;

/**
 * 商户 API 凭证表行映射。
 */
public record MerchantApiCredentialRow(
        long credentialId,
        long merchantId,
        String appKey,
        String secretCipher,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
