package com.mdyaipay.user.testsupport.merchant;

import com.mdyaipay.user.domain.merchant.Merchant;
import com.mdyaipay.user.domain.merchant.MerchantRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MapMerchantRepository implements MerchantRepository {

    private final Map<Long, Merchant> store = new ConcurrentHashMap<>();

    @Override
    public Merchant save(Merchant merchant) {
        store.put(merchant.getMerchantId(), merchant);
        return merchant;
    }

    @Override
    public Optional<Merchant> findById(long merchantId) {
        return Optional.ofNullable(store.get(merchantId));
    }
}
