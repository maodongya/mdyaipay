package com.mdyaipay.user.testsupport.merchant;

import com.mdyaipay.user.domain.merchant.Shop;
import com.mdyaipay.user.domain.merchant.ShopRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MapShopRepository implements ShopRepository {

    private final Map<Long, Shop> store = new ConcurrentHashMap<>();

    @Override
    public Shop save(Shop shop) {
        store.put(shop.getShopId(), shop);
        return shop;
    }

    @Override
    public Optional<Shop> findById(long shopId) {
        return Optional.ofNullable(store.get(shopId));
    }
}
