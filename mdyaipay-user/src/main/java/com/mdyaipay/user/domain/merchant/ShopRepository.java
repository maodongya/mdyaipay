package com.mdyaipay.user.domain.merchant;

import java.util.Optional;

/** 店铺持久化端口。 */
public interface ShopRepository {

    Shop save(Shop shop);

    Optional<Shop> findById(long shopId);
}
