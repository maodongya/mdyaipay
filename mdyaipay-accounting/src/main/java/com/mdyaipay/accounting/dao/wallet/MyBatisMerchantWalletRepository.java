package com.mdyaipay.accounting.dao.wallet;

import com.mdyaipay.accounting.dao.wallet.mybatis.mapper.MerchantWalletMapper;
import com.mdyaipay.accounting.dao.wallet.mybatis.row.MerchantWalletRow;
import com.mdyaipay.accounting.domain.wallet.MerchantWallet;
import com.mdyaipay.accounting.domain.wallet.MerchantWalletRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** {@link MerchantWalletRepository} 的 MyBatis 实现。 */
@Repository
public class MyBatisMerchantWalletRepository implements MerchantWalletRepository {

    private final MerchantWalletMapper merchantWalletMapper;

    public MyBatisMerchantWalletRepository(MerchantWalletMapper merchantWalletMapper) {
        this.merchantWalletMapper = Objects.requireNonNull(merchantWalletMapper);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<MerchantWallet> findByMerchantId(long merchantId) {
        MerchantWalletRow row = merchantWalletMapper.findByMerchantId(merchantId);
        return row == null ? Optional.empty() : Optional.of(toDomain(row));
    }

    /** {@inheritDoc} */
    @Override
    public void insert(MerchantWallet wallet) {
        merchantWalletMapper.insert(toRow(wallet));
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateWithVersion(MerchantWallet wallet, long expectedVersion) {
        int updated = merchantWalletMapper.updateWithVersion(
                wallet.getWalletId(),
                wallet.getAvailableAmount(),
                wallet.getPendingSettleAmount(),
                wallet.getWithdrawableAmount(),
                wallet.getVersion(),
                expectedVersion,
                wallet.getUpdatedAt());
        return updated > 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsTxnByBizKey(String bizKey) {
        return merchantWalletMapper.countTxnByBizKey(bizKey) > 0;
    }

    /** {@inheritDoc} */
    @Override
    public void insertTxn(long txnId, MerchantWallet wallet, String entryType, long amount, String bizKey) {
        merchantWalletMapper.insertTxn(
                txnId,
                wallet.getWalletId(),
                entryType,
                amount,
                bizKey,
                wallet.getAvailableAmount(),
                wallet.getPendingSettleAmount(),
                wallet.getWithdrawableAmount(),
                Instant.now());
    }

    private static MerchantWalletRow toRow(MerchantWallet wallet) {
        return new MerchantWalletRow(
                wallet.getWalletId(),
                wallet.getMerchantId(),
                wallet.getAvailableAmount(),
                wallet.getPendingSettleAmount(),
                wallet.getWithdrawableAmount(),
                wallet.getVersion(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt());
    }

    private static MerchantWallet toDomain(MerchantWalletRow row) {
        return MerchantWallet.rehydrate(
                row.walletId(),
                row.merchantId(),
                row.availableAmount(),
                row.pendingSettleAmount(),
                row.withdrawableAmount(),
                row.version(),
                row.createdAt(),
                row.updatedAt());
    }
}
