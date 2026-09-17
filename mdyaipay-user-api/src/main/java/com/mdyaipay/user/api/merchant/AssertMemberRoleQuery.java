package com.mdyaipay.user.api.merchant;

import java.io.Serializable;
import java.util.Objects;

/** 内网权限校验：判断 {@code userId} 在商户下是否满足 {@code requiredRole}（OWNER 满足全部）。 */
public final class AssertMemberRoleQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long merchantId;
    private final long userId;
    private final String requiredRole;

    public AssertMemberRoleQuery(long merchantId, long userId, String requiredRole) {
        if (merchantId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("merchantId and userId must be positive");
        }
        this.merchantId = merchantId;
        this.userId = userId;
        this.requiredRole = Objects.requireNonNull(requiredRole, "requiredRole must not be null");
    }

    public long getMerchantId() {
        return merchantId;
    }

    public long getUserId() {
        return userId;
    }

    public String getRequiredRole() {
        return requiredRole;
    }
}
