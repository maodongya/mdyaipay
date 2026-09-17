package com.mdyaipay.user.dao.merchant;

import com.mdyaipay.user.dao.merchant.mybatis.mapper.MerchantMapper;
import com.mdyaipay.user.dao.merchant.mybatis.row.MerchantRow;
import com.mdyaipay.user.domain.merchant.Merchant;
import com.mdyaipay.user.domain.merchant.MerchantRepository;
import com.mdyaipay.user.domain.merchant.MerchantStatus;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/** 商户表 {@code merchant} MyBatis 仓储。 */
@Repository
public class MyBatisMerchantRepository implements MerchantRepository {

    private final MerchantMapper merchantMapper;

    public MyBatisMerchantRepository(MerchantMapper merchantMapper) {
        this.merchantMapper = Objects.requireNonNull(merchantMapper);
    }

    @Override
    public Merchant save(Merchant merchant) {
        MerchantRow existing = merchantMapper.findById(merchant.getMerchantId());
        long version = existing == null ? 0L : existing.version();
        merchantMapper.upsert(new MerchantRow(
                merchant.getMerchantId(),
                merchant.getName(),
                merchant.getStatus().name(),
                version,
                merchant.getCreatedAt(),
                merchant.getUpdatedAt()));
        return merchant;
    }

    @Override
    public Optional<Merchant> findById(long merchantId) {
        MerchantRow row = merchantMapper.findById(merchantId);
        return row == null ? Optional.empty() : Optional.of(fromRow(row));
    }

    private static Merchant fromRow(MerchantRow row) {
        return Merchant.rehydrate(
                row.merchantId(),
                row.name(),
                MerchantStatus.valueOf(row.status()),
                row.createdAt(),
                row.updatedAt());
    }
}
