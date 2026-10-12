package com.mdyaipay.accounting.api.dubbo.wallet;

import com.mdyaipay.accounting.api.wallet.UserWalletEntryCommand;
import com.mdyaipay.accounting.api.wallet.UserWalletFacade;
import com.mdyaipay.accounting.api.wallet.UserWalletView;
import com.mdyaipay.accounting.observe.WalletAlertLog;
import com.mdyaipay.accounting.service.AccountingBusinessException;
import com.mdyaipay.accounting.service.wallet.UserWalletApplicationService;
import com.mdyaipay.accounting.service.wallet.WalletViewMapper;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * 用户钱包 Dubbo Provider：映射 {@link AccountingBusinessException} 为业务码。
 */
@Component
@DubboService(version = "1.0.0")
public class UserWalletFacadeImpl implements UserWalletFacade {

    private final UserWalletApplicationService userWalletApplicationService;

    public UserWalletFacadeImpl(UserWalletApplicationService userWalletApplicationService) {
        this.userWalletApplicationService = userWalletApplicationService;
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<UserWalletView> openWallet(long userId) {
        return run(() -> WalletViewMapper.toUserView(userWalletApplicationService.openWallet(userId)), false);
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<UserWalletView> getWallet(long userId) {
        return run(() -> WalletViewMapper.toUserView(userWalletApplicationService.getWallet(userId)), false);
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<UserWalletView> postEntry(UserWalletEntryCommand command) {
        return run(() -> WalletViewMapper.toUserView(userWalletApplicationService.postEntry(command)), true);
    }

    /**
     * 执行钱包调用；分录失败写报警事件，查询失败不另造分录事件。
     */
    private static <T> ApiResponse<T> run(java.util.concurrent.Callable<T> action, boolean entry) {
        try {
            return ApiResponse.ok(action.call());
        } catch (AccountingBusinessException ex) {
            if (entry) {
                WalletAlertLog.entryFailed(ex);
            }
            return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            WalletAlertLog.unexpected(ex);
            return ApiResponse.fail(500, ex.getMessage());
        }
    }
}
