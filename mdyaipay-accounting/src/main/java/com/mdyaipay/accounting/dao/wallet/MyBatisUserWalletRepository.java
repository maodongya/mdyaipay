package com.mdyaipay.accounting.dao.wallet;

import com.mdyaipay.accounting.dao.wallet.mybatis.mapper.UserWalletMapper;
import com.mdyaipay.accounting.dao.wallet.mybatis.row.UserWalletRow;
import com.mdyaipay.accounting.domain.wallet.UserWallet;
import com.mdyaipay.accounting.domain.wallet.UserWalletRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** {@link UserWalletRepository} 的 MyBatis 实现。 */
@Repository
public class MyBatisUserWalletRepository implements UserWalletRepository {

    private final UserWalletMapper userWalletMapper;

    public MyBatisUserWalletRepository(UserWalletMapper userWalletMapper) {
        this.userWalletMapper = Objects.requireNonNull(userWalletMapper);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<UserWallet> findByUserId(long userId) {
        UserWalletRow row = userWalletMapper.findByUserId(userId);
        return row == null ? Optional.empty() : Optional.of(toDomain(row));
    }

    /** {@inheritDoc} */
    @Override
    public void insert(UserWallet wallet) {
        userWalletMapper.insert(toRow(wallet));
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateWithVersion(UserWallet wallet, long expectedVersion) {
        int updated = userWalletMapper.updateWithVersion(
                wallet.getWalletId(),
                wallet.getBalance(),
                wallet.getFrozenAmount(),
                wallet.getVersion(),
                expectedVersion,
                wallet.getUpdatedAt());
        return updated > 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsTxnByBizKey(String bizKey) {
        return userWalletMapper.countTxnByBizKey(bizKey) > 0;
    }

    /** {@inheritDoc} */
    @Override
    public void insertTxn(long txnId, UserWallet wallet, String entryType, long amount, String bizKey) {
        userWalletMapper.insertTxn(
                txnId,
                wallet.getWalletId(),
                entryType,
                amount,
                bizKey,
                wallet.getBalance(),
                wallet.getFrozenAmount(),
                Instant.now());
    }

    private static UserWalletRow toRow(UserWallet wallet) {
        return new UserWalletRow(
                wallet.getWalletId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getFrozenAmount(),
                wallet.getVersion(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt());
    }

    private static UserWallet toDomain(UserWalletRow row) {
        return UserWallet.rehydrate(
                row.walletId(),
                row.userId(),
                row.balance(),
                row.frozenAmount(),
                row.version(),
                row.createdAt(),
                row.updatedAt());
    }
}
