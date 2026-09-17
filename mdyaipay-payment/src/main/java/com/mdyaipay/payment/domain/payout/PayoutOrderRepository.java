package com.mdyaipay.payment.domain.payout;

import java.util.Optional;

/**
 * 代付聚合持久化端口（按 {@code payoutNo} 幂等查询）。
 */
public interface PayoutOrderRepository {
    PayoutOrder save(PayoutOrder order);

    Optional<PayoutOrder> findByPayoutNo(String payoutNo);
}
