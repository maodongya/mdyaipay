package com.mdyaipay.user.domain.merchant;

import java.util.Optional;

/** 商户成员关系：{@code (merchantId, userId)} 唯一。 */
public interface MerchantMemberRepository {

    MerchantMember save(MerchantMember member);

    Optional<MerchantMember> findByMerchantIdAndUserId(long merchantId, long userId);
}
