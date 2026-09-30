package com.mdyaipay.accounting.domain.wallet;

import java.util.Optional;

/**
 * 用户钱包持久化端口。
 */
public interface UserWalletRepository {

    /** 按用户 ID 查询钱包。 */
    Optional<UserWallet> findByUserId(long userId);

    /** 插入新钱包。 */
    void insert(UserWallet wallet);

    /**
     * 乐观锁更新；影响行数为 0 时调用方应视为 {@link com.mdyaipay.accounting.api.AccountingErrorCodes#VERSION_CONFLICT}。
     *
     * @param expectedVersion 更新前读到的 version（未 +1 前的值）
     * @return 是否更新成功
     */
    boolean updateWithVersion(UserWallet wallet, long expectedVersion);

    /** 是否已有 bizKey 流水（全局幂等）。 */
    boolean existsTxnByBizKey(String bizKey);

    /** 写入用户钱包流水。 */
    void insertTxn(long txnId, UserWallet wallet, String entryType, long amount, String bizKey);
}
