package com.mdyaipay.payment.domain.payout;

import java.util.Optional;

public interface PayoutOrderRepository {
    PayoutOrder save(PayoutOrder order);

    Optional<PayoutOrder> findByPayoutNo(String payoutNo);
}
