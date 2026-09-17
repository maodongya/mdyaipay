package com.mdyaipay.user.testsupport.merchant;

import com.mdyaipay.user.domain.merchant.MerchantMember;
import com.mdyaipay.user.domain.merchant.MerchantMemberRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MapMerchantMemberRepository implements MerchantMemberRepository {

    private final Map<String, MerchantMember> store = new ConcurrentHashMap<>();

    @Override
    public MerchantMember save(MerchantMember member) {
        store.put(key(member.getMerchantId(), member.getUserId()), member);
        return member;
    }

    @Override
    public Optional<MerchantMember> findByMerchantIdAndUserId(long merchantId, long userId) {
        return Optional.ofNullable(store.get(key(merchantId, userId)));
    }

    private static String key(long merchantId, long userId) {
        return merchantId + ":" + userId;
    }
}
