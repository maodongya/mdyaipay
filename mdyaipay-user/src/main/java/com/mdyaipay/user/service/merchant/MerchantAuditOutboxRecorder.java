package com.mdyaipay.user.service.merchant;

import com.mdyaipay.user.domain.merchant.Merchant;

/**
 * 商户审核结果写入 Outbox，供 {@link com.mdyaipay.user.integration.mq.OutboxMessageRelay} 投递 MQ。
 */
public interface MerchantAuditOutboxRecorder {

    /** 审核通过后写入 Outbox，事件类型 AUDIT_APPROVED。 */
    void recordApproved(Merchant merchant, String auditor);

    /** 审核驳回后写入 Outbox，事件类型 AUDIT_REJECTED。 */
    void recordRejected(Merchant merchant, String auditor, String remark);
}
