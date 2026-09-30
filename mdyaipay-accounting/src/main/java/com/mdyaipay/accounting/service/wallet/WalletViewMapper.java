package com.mdyaipay.accounting.service.wallet;

import com.mdyaipay.accounting.api.wallet.MerchantWalletView;
import com.mdyaipay.accounting.api.wallet.UserWalletView;
import com.mdyaipay.accounting.domain.wallet.MerchantWallet;
import com.mdyaipay.accounting.domain.wallet.UserWallet;

/** 领域钱包与 API View 互转。 */
public final class WalletViewMapper {

    private WalletViewMapper() {
    }

    /** 用户钱包转 View。 */
    public static UserWalletView toUserView(UserWallet wallet) {
        return new UserWalletView(
                wallet.getWalletId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getFrozenAmount(),
                wallet.getVersion());
    }

    /** 商户钱包转 View。 */
    public static MerchantWalletView toMerchantView(MerchantWallet wallet) {
        return new MerchantWalletView(
                wallet.getWalletId(),
                wallet.getMerchantId(),
                wallet.getAvailableAmount(),
                wallet.getPendingSettleAmount(),
                wallet.getWithdrawableAmount(),
                wallet.getVersion());
    }
}
