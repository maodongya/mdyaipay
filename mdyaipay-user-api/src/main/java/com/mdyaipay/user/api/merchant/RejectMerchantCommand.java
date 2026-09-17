package com.mdyaipay.user.api.merchant;

import java.io.Serializable;
import java.util.Objects;

/** 平台审核驳回：{@code remark} 可选，供商户修改后再次提交。 */
public final class RejectMerchantCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long merchantId;
    private final String auditor;
    private final String remark;

    public RejectMerchantCommand(long merchantId, String auditor, String remark) {
        if (merchantId <= 0) {
            throw new IllegalArgumentException("merchantId must be positive");
        }
        this.merchantId = merchantId;
        this.auditor = Objects.requireNonNull(auditor, "auditor must not be null");
        this.remark = remark;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public String getAuditor() {
        return auditor;
    }

    public String getRemark() {
        return remark;
    }
}
