package com.mdyaipay.accounting.domain.wallet;

import java.util.Optional;

/**
 * 商户钱包持久化端口。
 */
public interface MerchantWalletRepository {

    /** 按商户 ID 查询钱包。 */
    Optional<MerchantWallet> findByMerchantId(long merchantId);

    /** 插入新商户钱包。 */
    void insert(MerchantWallet wallet);

    /** 乐观锁更新。 */
    boolean updateWithVersion(MerchantWallet wallet, long expectedVersion);

    /** 是否已有 bizKey 流水。 */
    boolean existsTxnByBizKey(String bizKey);

    /** 写入商户钱包流水。 */
    void insertTxn(long txnId, MerchantWallet wallet, String entryType, long amount, String bizKey);
}
