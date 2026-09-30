package com.mdyaipay.accounting.dao.wallet.mybatis.mapper;

import com.mdyaipay.accounting.dao.wallet.mybatis.row.UserWalletRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户钱包 MyBatis Mapper。
 */
@Mapper
public interface UserWalletMapper {

    /** 插入新钱包。 */
    void insert(UserWalletRow row);

    /** 按 userId 查询。 */
    UserWalletRow findByUserId(@Param("userId") long userId);

    /** 乐观锁更新。 */
    int updateWithVersion(
            @Param("walletId") long walletId,
            @Param("balance") long balance,
            @Param("frozenAmount") long frozenAmount,
            @Param("newVersion") long newVersion,
            @Param("expectedVersion") long expectedVersion,
            @Param("updatedAt") java.time.Instant updatedAt);

    /** 流水 bizKey 是否存在。 */
    int countTxnByBizKey(@Param("bizKey") String bizKey);

    /** 插入流水。 */
    void insertTxn(
            @Param("txnId") long txnId,
            @Param("walletId") long walletId,
            @Param("entryType") String entryType,
            @Param("amount") long amount,
            @Param("bizKey") String bizKey,
            @Param("balanceAfter") long balanceAfter,
            @Param("frozenAfter") long frozenAfter,
            @Param("createdAt") java.time.Instant createdAt);
}
