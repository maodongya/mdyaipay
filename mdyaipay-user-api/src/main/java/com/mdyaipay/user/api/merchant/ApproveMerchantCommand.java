package com.mdyaipay.user.api.merchant;

import java.io.Serializable;
import java.util.Objects;

/** 平台审核通过：{@code auditor} 为审核员标识（工号或系统账号）。 */
public final class ApproveMerchantCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long merchantId;
    private final String auditor;

    public ApproveMerchantCommand(long merchantId, String auditor) {
        if (merchantId <= 0) {
            throw new IllegalArgumentException("merchantId must be positive");
        }
        this.merchantId = merchantId;
        this.auditor = Objects.requireNonNull(auditor, "auditor must not be null");
    }

    public long getMerchantId() {
        return merchantId;
    }

    public String getAuditor() {
        return auditor;
    }
}
