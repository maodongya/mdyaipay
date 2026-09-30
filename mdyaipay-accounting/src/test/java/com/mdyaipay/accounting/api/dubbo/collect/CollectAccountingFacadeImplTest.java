package com.mdyaipay.accounting.api.dubbo.collect;

import com.mdyaipay.accounting.api.collect.PaymentCollectSuccessCommand;
import com.mdyaipay.accounting.service.wallet.MerchantWalletApplicationService;
import com.mdyaipay.accounting.testsupport.InMemoryMerchantWalletRepository;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * 收单入账 Dubbo Facade 单测：委托商户钱包待结算逻辑。
 */
class CollectAccountingFacadeImplTest {

    /** 带 merchantId 时应入账成功。 */
    @Test
    void shouldApplyPendingSettleViaFacade() {
        InMemoryMerchantWalletRepository repo = new InMemoryMerchantWalletRepository();
        MerchantWalletApplicationService walletService =
                new MerchantWalletApplicationService(repo, new SnowflakeIdGenerator(7L, 1L));
        CollectAccountingFacadeImpl facade = new CollectAccountingFacadeImpl(walletService);

        var response = facade.onPaymentCollectSuccess(new PaymentCollectSuccessCommand(
                "ORD-F1", 300L, 9001L, "MOCK", "QUICK_COLLECTION", Instant.now()));

        Assertions.assertEquals(ErrorCode.SUCCESS.getCode(), response.getCode());
        Assertions.assertEquals(300L, walletService.getWallet(9001L).getPendingSettleAmount());
    }

    /** 无 merchantId 时 no-op 仍 success。 */
    @Test
    void shouldSkipWhenMerchantIdMissing() {
        InMemoryMerchantWalletRepository repo = new InMemoryMerchantWalletRepository();
        MerchantWalletApplicationService walletService =
                new MerchantWalletApplicationService(repo, new SnowflakeIdGenerator(7L, 1L));
        CollectAccountingFacadeImpl facade = new CollectAccountingFacadeImpl(walletService);

        var response = facade.onPaymentCollectSuccess(new PaymentCollectSuccessCommand(
                "ORD-F2", 100L, null, "MOCK", "QUICK_COLLECTION", Instant.now()));

        Assertions.assertEquals(ErrorCode.SUCCESS.getCode(), response.getCode());
    }
}
