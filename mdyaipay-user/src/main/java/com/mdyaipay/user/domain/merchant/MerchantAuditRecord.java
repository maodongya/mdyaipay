package com.mdyaipay.user.domain.merchant;

import java.time.Instant;
import java.util.Objects;

/** 平台审核记录：{@code result} 为 APPROVED / REJECTED 等枚举字符串。 */
public final class MerchantAuditRecord {

    private final long recordId;
    private final long merchantId;
    private final String auditor;
    private final String result;
    private final String remark;
    private final Instant createdAt;

    public MerchantAuditRecord(
            long recordId,
            long merchantId,
            String auditor,
            String result,
            String remark,
            Instant createdAt) {
        if (recordId <= 0 || merchantId <= 0) {
            throw new IllegalArgumentException("recordId and merchantId must be positive");
        }
        this.recordId = recordId;
        this.merchantId = merchantId;
        this.auditor = Objects.requireNonNull(auditor, "auditor must not be null");
        this.result = Objects.requireNonNull(result, "result must not be null");
        this.remark = remark;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public long getRecordId() {
        return recordId;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public String getAuditor() {
        return auditor;
    }

    public String getResult() {
        return result;
    }

    public String getRemark() {
        return remark;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
