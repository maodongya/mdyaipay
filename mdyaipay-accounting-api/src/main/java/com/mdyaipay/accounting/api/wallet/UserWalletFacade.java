package com.mdyaipay.accounting.api.wallet;

import com.mdyaipay.tools.model.ApiResponse;

/**
 * 用户钱包 Dubbo 门面：开户、查询与记账。
 * <p>幂等：同一 {@link UserWalletEntryCommand#getBizKey()} 重复提交返回首次结果。</p>
 */
public interface UserWalletFacade {

    /** 为用户开户；已存在则返回现有钱包。幂等：按 userId。 */
    ApiResponse<UserWalletView> openWallet(long userId);

    /** 查询用户钱包；不存在返回业务错误 {@link com.mdyaipay.accounting.api.AccountingErrorCodes#WALLET_NOT_FOUND}。 */
    ApiResponse<UserWalletView> getWallet(long userId);

    /** 执行一笔用户钱包分录（入账/扣款/冻结/解冻）。 */
    ApiResponse<UserWalletView> postEntry(UserWalletEntryCommand command);
}
