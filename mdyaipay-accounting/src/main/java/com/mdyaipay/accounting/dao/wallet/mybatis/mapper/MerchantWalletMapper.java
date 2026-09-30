package com.mdyaipay.accounting.dao.wallet.mybatis.mapper;

import com.mdyaipay.accounting.dao.wallet.mybatis.row.MerchantWalletRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商户钱包 MyBatis Mapper。
 */
@Mapper
public interface MerchantWalletMapper {

    /** 插入新商户钱包。 */
    void insert(MerchantWalletRow row);

    /** 按 merchantId 查询。 */
    MerchantWalletRow findByMerchantId(@Param("merchantId") long merchantId);

    /** 乐观锁更新。 */
    int updateWithVersion(
            @Param("walletId") long walletId,
            @Param("availableAmount") long availableAmount,
            @Param("pendingSettleAmount") long pendingSettleAmount,
            @Param("withdrawableAmount") long withdrawableAmount,
            @Param("newVersion") long newVersion,
            @Param("expectedVersion") long expectedVersion,
            @Param("updatedAt") java.time.Instant updatedAt);

    /** 流水 bizKey 是否存在。 */
    int countTxnByBizKey(@Param("bizKey") String bizKey);

    /** 插入商户流水。 */
    void insertTxn(
            @Param("txnId") long txnId,
            @Param("walletId") long walletId,
            @Param("entryType") String entryType,
            @Param("amount") long amount,
            @Param("bizKey") String bizKey,
            @Param("availableAfter") long availableAfter,
            @Param("pendingSettleAfter") long pendingSettleAfter,
            @Param("withdrawableAfter") long withdrawableAfter,
            @Param("createdAt") java.time.Instant createdAt);
}
