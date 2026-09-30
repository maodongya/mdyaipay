package com.mdyaipay.accounting.service.wallet;

import com.mdyaipay.accounting.api.mq.PaymentCollectSettledMessage;
import com.mdyaipay.accounting.api.wallet.MerchantWalletEntryCommand;
import com.mdyaipay.accounting.api.wallet.MerchantWalletEntryType;
import com.mdyaipay.accounting.domain.wallet.MerchantWallet;
import com.mdyaipay.accounting.testsupport.InMemoryMerchantWalletRepository;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * 商户钱包应用服务单测：待结算入账、结算划转与 MQ 消息幂等。
 */
class MerchantWalletApplicationServiceTest {

    private static MerchantWalletApplicationService newService(InMemoryMerchantWalletRepository repo) {
        return new MerchantWalletApplicationService(repo, new SnowflakeIdGenerator(5L, 1L));
    }

    /** 收单成功消息应增加待结算金额。 */
    @Test
    void shouldCreditPendingSettleFromPaymentMessage() {
        InMemoryMerchantWalletRepository repo = new InMemoryMerchantWalletRepository();
        MerchantWalletApplicationService service = newService(repo);
        PaymentCollectSettledMessage message = new PaymentCollectSettledMessage(
                "ORD-1", 500L, 1001L, "MOCK", "QUICK_COLLECTION", Instant.now());
        service.onPaymentCollectSettled(message);
        MerchantWallet wallet = service.getWallet(1001L);
        Assertions.assertEquals(500L, wallet.getPendingSettleAmount());
    }

    /** 同一 orderNo 重复消息不重复入账。 */
    @Test
    void shouldBeIdempotentForSameOrderNo() {
        InMemoryMerchantWalletRepository repo = new InMemoryMerchantWalletRepository();
        MerchantWalletApplicationService service = newService(repo);
        PaymentCollectSettledMessage message = new PaymentCollectSettledMessage(
                "ORD-2", 100L, 2002L, "MOCK", "QUICK_COLLECTION", Instant.now());
        service.onPaymentCollectSettled(message);
        service.onPaymentCollectSettled(message);
        Assertions.assertEquals(100L, service.getWallet(2002L).getPendingSettleAmount());
    }

    /** 待结算可划转到可提现。 */
    @Test
    void shouldTransferPendingToWithdrawable() {
        InMemoryMerchantWalletRepository repo = new InMemoryMerchantWalletRepository();
        MerchantWalletApplicationService service = newService(repo);
        service.postEntry(new MerchantWalletEntryCommand(
                3003L, "collect:ORD-3", MerchantWalletEntryType.PENDING_SETTLE_CREDIT.name(), 200L));
        service.postEntry(new MerchantWalletEntryCommand(
                3003L, "settle:ORD-3", MerchantWalletEntryType.PENDING_TO_WITHDRAWABLE.name(), 200L));
        MerchantWallet wallet = service.getWallet(3003L);
        Assertions.assertEquals(0L, wallet.getPendingSettleAmount());
        Assertions.assertEquals(200L, wallet.getWithdrawableAmount());
    }
}
