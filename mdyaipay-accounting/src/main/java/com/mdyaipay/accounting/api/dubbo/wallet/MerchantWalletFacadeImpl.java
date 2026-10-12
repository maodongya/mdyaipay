package com.mdyaipay.accounting.api.dubbo.wallet;

import com.mdyaipay.accounting.api.wallet.MerchantWalletEntryCommand;
import com.mdyaipay.accounting.api.wallet.MerchantWalletFacade;
import com.mdyaipay.accounting.api.wallet.MerchantWalletView;
import com.mdyaipay.accounting.observe.WalletAlertLog;
import com.mdyaipay.accounting.service.AccountingBusinessException;
import com.mdyaipay.accounting.service.wallet.MerchantWalletApplicationService;
import com.mdyaipay.accounting.service.wallet.WalletViewMapper;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * 商户钱包 Dubbo Provider。
 */
@Component
@DubboService(version = "1.0.0")
public class MerchantWalletFacadeImpl implements MerchantWalletFacade {

    private final MerchantWalletApplicationService merchantWalletApplicationService;

    public MerchantWalletFacadeImpl(MerchantWalletApplicationService merchantWalletApplicationService) {
        this.merchantWalletApplicationService = merchantWalletApplicationService;
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<MerchantWalletView> openWallet(long merchantId) {
        return run(() -> WalletViewMapper.toMerchantView(merchantWalletApplicationService.openWallet(merchantId)), false);
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<MerchantWalletView> getWallet(long merchantId) {
        return run(() -> WalletViewMapper.toMerchantView(merchantWalletApplicationService.getWallet(merchantId)), false);
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<MerchantWalletView> postEntry(MerchantWalletEntryCommand command) {
        return run(() -> WalletViewMapper.toMerchantView(merchantWalletApplicationService.postEntry(command)), true);
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
