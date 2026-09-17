package com.mdyaipay.user.dao.merchant.mybatis.row;

import java.time.Instant;

/**
 * 商户审核记录表行映射。
 */
public record MerchantAuditRecordRow(
        long recordId,
        long merchantId,
        String auditor,
        String result,
        String remark,
        Instant createdAt) {
}
