package com.mdyaipay.user.domain.merchant;

import java.util.Optional;

/** 商户聚合持久化端口。 */
public interface MerchantRepository {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(long merchantId);
}
