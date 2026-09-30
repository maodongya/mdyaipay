package com.mdyaipay.accounting.testsupport;

import com.mdyaipay.accounting.domain.wallet.MerchantWallet;
import com.mdyaipay.accounting.domain.wallet.MerchantWalletRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 商户钱包内存仓储：单测用。 */
public final class InMemoryMerchantWalletRepository implements MerchantWalletRepository {

    private final Map<Long, MerchantWallet> byMerchant = new ConcurrentHashMap<>();
    private final Set<String> bizKeys = ConcurrentHashMap.newKeySet();

    /** {@inheritDoc} */
    @Override
    public Optional<MerchantWallet> findByMerchantId(long merchantId) {
        MerchantWallet stored = byMerchant.get(merchantId);
        return stored == null ? Optional.empty() : Optional.of(copy(stored));
    }

    /** {@inheritDoc} */
    @Override
    public void insert(MerchantWallet wallet) {
        byMerchant.put(wallet.getMerchantId(), copy(wallet));
    }

    /** {@inheritDoc} */
    @Override
    public boolean updateWithVersion(MerchantWallet wallet, long expectedVersion) {
        MerchantWallet current = byMerchant.get(wallet.getMerchantId());
        if (current == null || current.getVersion() != expectedVersion) {
            return false;
        }
        byMerchant.put(wallet.getMerchantId(), copy(wallet));
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsTxnByBizKey(String bizKey) {
        return bizKeys.contains(bizKey);
    }

    /** {@inheritDoc} */
    @Override
    public void insertTxn(long txnId, MerchantWallet wallet, String entryType, long amount, String bizKey) {
        bizKeys.add(bizKey);
        byMerchant.put(wallet.getMerchantId(), copy(wallet));
    }

    private static MerchantWallet copy(MerchantWallet wallet) {
        return MerchantWallet.rehydrate(
                wallet.getWalletId(),
                wallet.getMerchantId(),
                wallet.getAvailableAmount(),
                wallet.getPendingSettleAmount(),
                wallet.getWithdrawableAmount(),
                wallet.getVersion(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt());
    }
}
