package com.mdyaipay.accounting.dao.wallet.mybatis.row;

import java.time.Instant;

/** merchant_wallet 表行映射。 */
public record MerchantWalletRow(
        long walletId,
        long merchantId,
        long availableAmount,
        long pendingSettleAmount,
        long withdrawableAmount,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
