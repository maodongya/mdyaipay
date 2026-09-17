package com.mdyaipay.user.dao.merchant;

import com.mdyaipay.user.dao.merchant.mybatis.mapper.MerchantApiCredentialMapper;
import com.mdyaipay.user.dao.merchant.mybatis.row.MerchantApiCredentialRow;
import com.mdyaipay.user.domain.merchant.CredentialStatus;
import com.mdyaipay.user.domain.merchant.MerchantApiCredential;
import com.mdyaipay.user.domain.merchant.MerchantApiCredentialRepository;
import com.mdyaipay.user.integration.crypto.AesSecretCipher;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * 商户凭证 MyBatis 仓储：表 {@code merchant_api_credential}，Secret 落库为 AES 密文，验签时在内存解密（不落日志）。
 */
@Repository
public class MyBatisMerchantApiCredentialRepository implements MerchantApiCredentialRepository {

    private final MerchantApiCredentialMapper credentialMapper;
    private final AesSecretCipher aesSecretCipher;

    public MyBatisMerchantApiCredentialRepository(
            MerchantApiCredentialMapper credentialMapper,
            AesSecretCipher aesSecretCipher) {
        this.credentialMapper = Objects.requireNonNull(credentialMapper);
        this.aesSecretCipher = Objects.requireNonNull(aesSecretCipher);
    }

    @Override
    public MerchantApiCredential save(MerchantApiCredential credential) {
        String cipher = aesSecretCipher.encrypt(credential.getSecretPlain());
        credentialMapper.upsert(new MerchantApiCredentialRow(
                credential.getCredentialId(),
                credential.getMerchantId(),
                credential.getAppKey(),
                cipher,
                credential.getStatus().name(),
                credential.getCreatedAt(),
                credential.getUpdatedAt()));
        return credential;
    }

    @Override
    public Optional<MerchantApiCredential> findByAppKey(String appKey) {
        MerchantApiCredentialRow row = credentialMapper.findByAppKey(appKey);
        if (row == null) {
            return Optional.empty();
        }
        String plain = aesSecretCipher.decrypt(row.secretCipher());
        return Optional.of(new MerchantApiCredential(
                row.credentialId(),
                row.merchantId(),
                row.appKey(),
                plain,
                CredentialStatus.valueOf(row.status()),
                row.createdAt(),
                row.updatedAt()));
    }
}
