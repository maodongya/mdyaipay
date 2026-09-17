package com.mdyaipay.user.testsupport.merchant;

import com.mdyaipay.user.domain.merchant.Merchant;
import com.mdyaipay.user.service.merchant.MerchantAuditOutboxRecorder;

/** 单测用：不写入 Outbox。 */
public final class NoOpMerchantAuditOutboxRecorder implements MerchantAuditOutboxRecorder {

    @Override
    public void recordApproved(Merchant merchant, String auditor) {
        /* no-op */
    }

    @Override
    public void recordRejected(Merchant merchant, String auditor, String remark) {
        /* no-op */
    }
}
