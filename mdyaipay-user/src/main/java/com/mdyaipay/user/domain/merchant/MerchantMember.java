package com.mdyaipay.user.domain.merchant;

import java.time.Instant;
import java.util.Objects;

/** 商户成员：关联统一用户主体 {@code userId}。 */
public final class MerchantMember {

    private final long memberId;
    private final long merchantId;
    private final long userId;
    private final MerchantMemberRole role;
    private final Instant createdAt;

    public MerchantMember(long memberId, long merchantId, long userId, MerchantMemberRole role) {
        this(memberId, merchantId, userId, role, Instant.now());
    }

    private MerchantMember(
            long memberId,
            long merchantId,
            long userId,
            MerchantMemberRole role,
            Instant createdAt) {
        if (memberId <= 0 || merchantId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("ids must be positive");
        }
        this.memberId = memberId;
        this.merchantId = merchantId;
        this.userId = userId;
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public long getMemberId() {
        return memberId;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public long getUserId() {
        return userId;
    }

    public MerchantMemberRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
