package com.mdyaipay.user.api.merchant;

import java.io.Serializable;

/** 权限校验结果：{@code allowed} 与成员实际 {@code actualRole}（非成员时为 null）。 */
public final class AssertMemberRoleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean allowed;
    private final String actualRole;

    public AssertMemberRoleResult(boolean allowed, String actualRole) {
        this.allowed = allowed;
        this.actualRole = actualRole;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getActualRole() {
        return actualRole;
    }
}
