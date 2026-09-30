package com.mdyaipay.accounting.service.wallet;

import com.mdyaipay.accounting.api.AccountingErrorCodes;
import com.mdyaipay.accounting.api.wallet.UserWalletEntryCommand;
import com.mdyaipay.accounting.api.wallet.UserWalletEntryType;
import com.mdyaipay.accounting.domain.wallet.UserWallet;
import com.mdyaipay.accounting.domain.wallet.UserWalletRepository;
import com.mdyaipay.accounting.service.AccountingBusinessException;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * 用户钱包应用编排：开户、幂等分录与乐观锁重试。
 */
@Service
public class UserWalletApplicationService {

    private static final int MAX_VERSION_RETRIES = 5;

    private final UserWalletRepository userWalletRepository;
    private final SnowflakeIdGenerator idGenerator;

    public UserWalletApplicationService(UserWalletRepository userWalletRepository, SnowflakeIdGenerator idGenerator) {
        this.userWalletRepository = Objects.requireNonNull(userWalletRepository);
        this.idGenerator = Objects.requireNonNull(idGenerator);
    }

    /**
     * 为用户开户；已存在则直接返回。
     * <p>幂等：同一 userId 多次调用结果一致。</p>
     */
    @Transactional
    public UserWallet openWallet(long userId) {
        if (userId <= 0) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "userId must be positive");
        }
        return userWalletRepository.findByUserId(userId).orElseGet(() -> createWallet(userId));
    }

    /** 查询钱包；不存在抛业务异常。 */
    @Transactional(readOnly = true)
    public UserWallet getWallet(long userId) {
        return userWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountingBusinessException(
                        AccountingErrorCodes.WALLET_NOT_FOUND, "user wallet not found: " + userId));
    }

    /**
     * 执行分录；{@link UserWalletEntryCommand#getBizKey()} 全局幂等。
     */
    @Transactional
    public UserWallet postEntry(UserWalletEntryCommand command) {
        validateEntryCommand(command);
        if (userWalletRepository.existsTxnByBizKey(command.getBizKey())) {
            return getWallet(command.getUserId());
        }
        UserWalletEntryType entryType = UserWalletEntryType.valueOf(command.getEntryType());
        for (int attempt = 0; attempt < MAX_VERSION_RETRIES; attempt++) {
            UserWallet wallet = openWallet(command.getUserId());
            long expectedVersion = wallet.getVersion();
            wallet.applyEntry(entryType, command.getAmount());
            if (userWalletRepository.updateWithVersion(wallet, expectedVersion)) {
                userWalletRepository.insertTxn(
                        idGenerator.nextId(),
                        wallet,
                        entryType.name(),
                        command.getAmount(),
                        command.getBizKey());
                return wallet;
            }
        }
        throw new AccountingBusinessException(AccountingErrorCodes.VERSION_CONFLICT, "user wallet update conflict");
    }

    private UserWallet createWallet(long userId) {
        Instant now = Instant.now();
        UserWallet wallet = UserWallet.open(idGenerator.nextId(), userId, now);
        userWalletRepository.insert(wallet);
        return wallet;
    }

    private static void validateEntryCommand(UserWalletEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.getUserId() <= 0) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "userId must be positive");
        }
        if (command.getBizKey() == null || command.getBizKey().isBlank()) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "bizKey required");
        }
        if (command.getEntryType() == null || command.getEntryType().isBlank()) {
            throw new AccountingBusinessException(AccountingErrorCodes.INVALID_ARGUMENT, "entryType required");
        }
    }
}
