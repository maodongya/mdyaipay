package com.mdyaipay.user.domain.merchant;

import java.time.Instant;
import java.util.Objects;

/**
 * 商户开放 API 密钥实体，持久化表 {@code merchant_api_credential}。
 * <p>验签时使用 {@code secretPlain}（由 dao 从 {@code secret_cipher} 解密），禁止写入日志。</p>
 */
public final class MerchantApiCredential {

    private final long credentialId;
    private final long merchantId;
    private final String appKey;
    private final String secretPlain;
    private CredentialStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public MerchantApiCredential(
            long credentialId,
            long merchantId,
            String appKey,
            String secretPlain,
            CredentialStatus status,
            Instant createdAt,
            Instant updatedAt) {
        if (credentialId <= 0 || merchantId <= 0) {
            throw new IllegalArgumentException("credentialId and merchantId must be positive");
        }
        this.credentialId = credentialId;
        this.merchantId = merchantId;
        this.appKey = Objects.requireNonNull(appKey, "appKey must not be null");
        this.secretPlain = Objects.requireNonNull(secretPlain, "secretPlain must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public boolean isActive() {
        return status == CredentialStatus.ACTIVE;
    }

    public long getCredentialId() {
        return credentialId;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getSecretPlain() {
        return secretPlain;
    }

    public CredentialStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
