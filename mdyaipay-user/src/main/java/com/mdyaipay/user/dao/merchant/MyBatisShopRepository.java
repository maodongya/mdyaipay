package com.mdyaipay.user.dao.merchant;

import com.mdyaipay.user.dao.merchant.mybatis.mapper.ShopMapper;
import com.mdyaipay.user.dao.merchant.mybatis.row.ShopRow;
import com.mdyaipay.user.domain.merchant.Shop;
import com.mdyaipay.user.domain.merchant.ShopOperatingStatus;
import com.mdyaipay.user.domain.merchant.ShopRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/** 店铺表 {@code shop} MyBatis 仓储。 */
@Repository
public class MyBatisShopRepository implements ShopRepository {

    private final ShopMapper shopMapper;

    public MyBatisShopRepository(ShopMapper shopMapper) {
        this.shopMapper = Objects.requireNonNull(shopMapper);
    }

    @Override
    public Shop save(Shop shop) {
        shopMapper.upsert(new ShopRow(
                shop.getShopId(),
                shop.getMerchantId(),
                shop.getShopName(),
                shop.getCategory(),
                shop.getOperatingStatus().name(),
                shop.getCreatedAt(),
                shop.getUpdatedAt()));
        return shop;
    }

    @Override
    public Optional<Shop> findById(long shopId) {
        ShopRow row = shopMapper.findById(shopId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(Shop.rehydrate(
                row.shopId(),
                row.merchantId(),
                row.shopName(),
                row.category(),
                ShopOperatingStatus.valueOf(row.operatingStatus()),
                row.createdAt(),
                row.updatedAt()));
    }
}
