package com.mdyaipay.accounting.api.dubbo.wallet;

import com.mdyaipay.accounting.api.wallet.MerchantWalletEntryCommand;
import com.mdyaipay.accounting.api.wallet.MerchantWalletFacade;
import com.mdyaipay.accounting.api.wallet.MerchantWalletView;
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
        return run(() -> WalletViewMapper.toMerchantView(merchantWalletApplicationService.openWallet(merchantId)));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<MerchantWalletView> getWallet(long merchantId) {
        return run(() -> WalletViewMapper.toMerchantView(merchantWalletApplicationService.getWallet(merchantId)));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<MerchantWalletView> postEntry(MerchantWalletEntryCommand command) {
        return run(() -> WalletViewMapper.toMerchantView(merchantWalletApplicationService.postEntry(command)));
    }

    private static <T> ApiResponse<T> run(java.util.concurrent.Callable<T> action) {
        try {
            return ApiResponse.ok(action.call());
        } catch (AccountingBusinessException ex) {
            return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.fail(500, ex.getMessage());
        }
    }
}
