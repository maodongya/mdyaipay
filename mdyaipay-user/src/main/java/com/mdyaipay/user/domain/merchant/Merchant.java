package com.mdyaipay.user.domain.merchant;

import java.time.Instant;
import java.util.Objects;

/**
 * 商户聚合根：审核与运营状态迁移。
 * <p>不负责验签与 Dubbo；状态仅通过本类 public 方法变更。</p>
 */
public final class Merchant {

    private final long merchantId;
    private final String name;
    private MerchantStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Merchant(long merchantId, String name) {
        this(merchantId, name, MerchantStatus.DRAFT, Instant.now(), Instant.now());
    }

    private Merchant(long merchantId, String name, MerchantStatus status, Instant createdAt, Instant updatedAt) {
        if (merchantId <= 0) {
            throw new IllegalArgumentException("merchantId must be positive");
        }
        this.merchantId = merchantId;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /** 从 dao 重建聚合，不触发新建时的业务校验以外的状态迁移。 */
    public static Merchant rehydrate(
            long merchantId,
            String name,
            MerchantStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new Merchant(merchantId, name, status, createdAt, updatedAt);
    }

    /** 商户提交审核：DRAFT / REJECTED → PENDING。 */
    public void submitAudit() {
        if (status != MerchantStatus.DRAFT && status != MerchantStatus.REJECTED) {
            throw new IllegalStateException("submitAudit only from DRAFT or REJECTED, current=" + status);
        }
        status = MerchantStatus.PENDING;
        touch();
    }

    /** 平台审核通过：PENDING → APPROVED（启用需再调 {@link #enable()}）。 */
    public void approve() {
        if (status != MerchantStatus.PENDING) {
            throw new IllegalStateException("approve only from PENDING, current=" + status);
        }
        status = MerchantStatus.APPROVED;
        touch();
    }

    /** 平台审核驳回：PENDING → REJECTED。 */
    public void reject() {
        if (status != MerchantStatus.PENDING) {
            throw new IllegalStateException("reject only from PENDING, current=" + status);
        }
        status = MerchantStatus.REJECTED;
        touch();
    }

    /** 运营启用：APPROVED / DISABLED → ENABLED。 */
    public void enable() {
        if (status != MerchantStatus.APPROVED && status != MerchantStatus.DISABLED) {
            throw new IllegalStateException("enable only from APPROVED or DISABLED, current=" + status);
        }
        status = MerchantStatus.ENABLED;
        touch();
    }

    /** 运营禁用：ENABLED → DISABLED，同时关闭开放 API。 */
    public void disable() {
        if (status != MerchantStatus.ENABLED) {
            throw new IllegalStateException("disable only from ENABLED, current=" + status);
        }
        status = MerchantStatus.DISABLED;
        touch();
    }

    /** 禁用运营态仍禁止开放 API；草稿/待审/驳回态允许调审核类接口。 */
    public boolean allowsMerchantApi() {
        return status != MerchantStatus.DISABLED;
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public long getMerchantId() {
        return merchantId;
    }

    public String getName() {
        return name;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
