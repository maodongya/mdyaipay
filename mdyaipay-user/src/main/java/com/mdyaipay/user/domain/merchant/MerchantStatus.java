package com.mdyaipay.user.domain.merchant;

/** 商户生命周期：草稿 → 待审 → 通过/驳回 → 运营启用/禁用。 */
public enum MerchantStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    ENABLED,
    DISABLED
}
