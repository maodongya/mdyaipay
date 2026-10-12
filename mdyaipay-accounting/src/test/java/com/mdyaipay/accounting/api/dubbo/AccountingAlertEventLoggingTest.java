package com.mdyaipay.accounting.api.dubbo;

import com.mdyaipay.accounting.api.AccountingErrorCodes;
import com.mdyaipay.accounting.api.dubbo.wallet.MerchantWalletFacadeImpl;
import com.mdyaipay.accounting.api.wallet.MerchantWalletEntryCommand;
import com.mdyaipay.accounting.api.wallet.MerchantWalletEntryType;
import com.mdyaipay.accounting.observe.WalletAlertLog;
import com.mdyaipay.accounting.service.AccountingBusinessException;
import com.mdyaipay.accounting.service.wallet.MerchantWalletApplicationService;
import com.mdyaipay.accounting.testsupport.InMemoryMerchantWalletRepository;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import com.mdyaipay.tools.logging.AlertEvents;
import com.mdyaipay.tools.logging.LogMessageCapture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账务失败事件日志。
 */
class AccountingAlertEventLoggingTest {

    /** 非法分录记录 wallet_entry_failed。 */
    @Test
    void shouldLogWalletEntryFailed() {
        MerchantWalletFacadeImpl facade = new MerchantWalletFacadeImpl(
                new MerchantWalletApplicationService(
                        new InMemoryMerchantWalletRepository(), new SnowflakeIdGenerator(8L, 1L)));
        List<String> messages = LogMessageCapture.capture(WalletAlertLog.class, () ->
                facade.postEntry(new MerchantWalletEntryCommand(
                        0L, "biz-1", MerchantWalletEntryType.PENDING_SETTLE_CREDIT.name(), 100L)));
        assertTrue(messages.stream().anyMatch(line ->
                line.contains(AlertEvents.failure(AlertEvents.WALLET_ENTRY_FAILED))));
    }

    /** 乐观锁冲突记录独立事件。 */
    @Test
    void shouldLogWalletVersionConflict() {
        List<String> messages = LogMessageCapture.capture(WalletAlertLog.class, () ->
                WalletAlertLog.entryFailed(new AccountingBusinessException(
                        AccountingErrorCodes.VERSION_CONFLICT, "conflict")));
        assertTrue(messages.stream().anyMatch(line ->
                line.contains(AlertEvents.failure(AlertEvents.WALLET_VERSION_CONFLICT))));
    }
}
