package com.mdyaipay.user.domain.merchant;

/** 平台审核流水：只追加，不修改历史记录。 */
public interface MerchantAuditRecordRepository {

    MerchantAuditRecord save(MerchantAuditRecord record);
}
