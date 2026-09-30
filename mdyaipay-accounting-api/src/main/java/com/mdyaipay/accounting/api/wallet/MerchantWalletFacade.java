package com.mdyaipay.accounting.api.wallet;

import com.mdyaipay.tools.model.ApiResponse;

/**
 * 商户钱包 Dubbo 门面：待结算、可提现与提现扣款。
 */
public interface MerchantWalletFacade {

    /** 为商户开户；已存在则返回现有钱包。 */
    ApiResponse<MerchantWalletView> openWallet(long merchantId);

    /** 查询商户钱包。 */
    ApiResponse<MerchantWalletView> getWallet(long merchantId);

    /** 执行商户钱包分录（待结算入账、结算划转、提现等）。 */
    ApiResponse<MerchantWalletView> postEntry(MerchantWalletEntryCommand command);
}
