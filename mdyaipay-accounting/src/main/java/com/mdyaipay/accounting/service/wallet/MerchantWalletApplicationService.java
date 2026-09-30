package com.mdyaipay.accounting.service.wallet;

import com.mdyaipay.accounting.api.AccountingErrorCodes;
import com.mdyaipay.accounting.api.mq.PaymentCollectSettledMessage;
import com.mdyaipay.accounting.api.wallet.MerchantWalletEntryCommand;
import com.mdyaipay.accounting.api.wallet.MerchantWalletEntryType;
import com.mdyaipay.accounting.domain.wallet.MerchantWallet;
import com.mdyaipay.accounting.domain.wallet.MerchantWalletRepository;
import com.mdyaipay.accounting.service.AccountingBusinessException;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * 商户钱包应用编排：待结算入账、结算划转与 MQ 驱动入账。
 */
@Service
public class MerchantWalletApplicationService {

    private static final int MAX_VERSION_RETRIES = 5;
    private static final String COLLECT_BIZ_KEY_PREFIX = "collect:";

    private final MerchantWalletRepository merchantWalletRepository;
    private final SnowflakeIdGenerator idGenerator;

    public MerchantWalletApplicationService(
            MerchantWalletRepository merchantWalletRepository, SnowflakeIdGenerator idGenerator) {
        this.merchantWalletRepository = Objects.requireNonNull(merchantWalletRepository);
        this.idGenerator = Objects.requireNonNull(idGenerator);
    }

    /** 为商户开户；已存在则返回现有钱包。 */
    @Transactional
    public MerchantWallet openWallet(long merchantId) {
        if (merchantId <= 0) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "merchantId must be positive");
        }
        return merchantWalletRepository.findByMerchantId(merchantId).orElseGet(() -> createWallet(merchantId));
    }

    /** 查询商户钱包。 */
    @Transactional(readOnly = true)
    public MerchantWallet getWallet(long merchantId) {
        return merchantWalletRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new AccountingBusinessException(
                        AccountingErrorCodes.WALLET_NOT_FOUND, "merchant wallet not found: " + merchantId));
    }

    /** 执行商户分录；bizKey 全局幂等。 */
    @Transactional
    public MerchantWallet postEntry(MerchantWalletEntryCommand command) {
        validateEntryCommand(command);
        if (merchantWalletRepository.existsTxnByBizKey(command.getBizKey())) {
            return getWallet(command.getMerchantId());
        }
        MerchantWalletEntryType entryType = MerchantWalletEntryType.valueOf(command.getEntryType());
        for (int attempt = 0; attempt < MAX_VERSION_RETRIES; attempt++) {
            MerchantWallet wallet = openWallet(command.getMerchantId());
            long expectedVersion = wallet.getVersion();
            wallet.applyEntry(entryType, command.getAmount());
            if (merchantWalletRepository.updateWithVersion(wallet, expectedVersion)) {
                merchantWalletRepository.insertTxn(
                        idGenerator.nextId(),
                        wallet,
                        entryType.name(),
                        command.getAmount(),
                        command.getBizKey());
                return wallet;
            }
        }
        throw new AccountingBusinessException(AccountingErrorCodes.VERSION_CONFLICT, "merchant wallet update conflict");
    }

    /**
     * 消费收单成功消息：有 merchantId 时增加待结算。
     * <p>幂等键 {@code collect:{orderNo}}。</p>
     */
    @Transactional
    public void onPaymentCollectSettled(PaymentCollectSettledMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        if (message.getMerchantId() == null || message.getMerchantId() <= 0) {
            return;
        }
        if (message.getAmount() <= 0 || message.getOrderNo() == null || message.getOrderNo().isBlank()) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "invalid collect settled message");
        }
        String bizKey = COLLECT_BIZ_KEY_PREFIX + message.getOrderNo();
        MerchantWalletEntryCommand command = new MerchantWalletEntryCommand(
                message.getMerchantId(),
                bizKey,
                MerchantWalletEntryType.PENDING_SETTLE_CREDIT.name(),
                message.getAmount());
        postEntry(command);
    }

    private MerchantWallet createWallet(long merchantId) {
        Instant now = Instant.now();
        MerchantWallet wallet = MerchantWallet.open(idGenerator.nextId(), merchantId, now);
        merchantWalletRepository.insert(wallet);
        return wallet;
    }

    private static void validateEntryCommand(MerchantWalletEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.getMerchantId() <= 0) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "merchantId must be positive");
        }
        if (command.getBizKey() == null || command.getBizKey().isBlank()) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "bizKey required");
        }
        if (command.getEntryType() == null || command.getEntryType().isBlank()) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "entryType required");
        }
    }
}
