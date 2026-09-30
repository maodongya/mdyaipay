package com.mdyaipay.accounting.dao.wallet.mybatis.row;

import java.time.Instant;

/** user_wallet 表行映射。 */
public record UserWalletRow(
        long walletId,
        long userId,
        long balance,
        long frozenAmount,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
