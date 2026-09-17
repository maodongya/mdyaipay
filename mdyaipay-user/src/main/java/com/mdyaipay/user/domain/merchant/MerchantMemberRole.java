package com.mdyaipay.user.domain.merchant;

/** 商户成员角色：OWNER 最高；内网校验时 OWNER 视为满足任意 requiredRole。 */
public enum MerchantMemberRole {
    OWNER,
    OP,
    FIN;

    /** 是否满足内网 {@code requiredRole} 校验（OWNER 恒为 true）。 */
    public boolean satisfies(MerchantMemberRole required) {
        if (required == null) {
            return false;
        }
        if (this == OWNER) {
            return true;
        }
        return this == required;
    }
}
