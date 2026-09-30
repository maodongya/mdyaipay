package com.mdyaipay.accounting.api.dubbo.collect;

import com.mdyaipay.accounting.api.collect.CollectAccountingFacade;
import com.mdyaipay.accounting.api.collect.PaymentCollectSuccessCommand;
import com.mdyaipay.accounting.api.mq.PaymentCollectSettledMessage;
import com.mdyaipay.accounting.service.AccountingBusinessException;
import com.mdyaipay.accounting.service.wallet.MerchantWalletApplicationService;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * 收单入账 Dubbo Provider：转调商户钱包待结算逻辑。
 */
@Component
@DubboService(version = "1.0.0")
public class CollectAccountingFacadeImpl implements CollectAccountingFacade {

    private final MerchantWalletApplicationService merchantWalletApplicationService;

    public CollectAccountingFacadeImpl(MerchantWalletApplicationService merchantWalletApplicationService) {
        this.merchantWalletApplicationService = merchantWalletApplicationService;
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<Void> onPaymentCollectSuccess(PaymentCollectSuccessCommand command) {
        try {
            merchantWalletApplicationService.onPaymentCollectSettled(toMessage(command));
            return ApiResponse.ok(null);
        } catch (AccountingBusinessException ex) {
            return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.fail(500, ex.getMessage());
        }
    }

    private static PaymentCollectSettledMessage toMessage(PaymentCollectSuccessCommand command) {
        return new PaymentCollectSettledMessage(
                command.getOrderNo(),
                command.getAmount(),
                command.getMerchantId(),
                command.getChannel(),
                command.getProductType(),
                command.getSettledAt());
    }
}
