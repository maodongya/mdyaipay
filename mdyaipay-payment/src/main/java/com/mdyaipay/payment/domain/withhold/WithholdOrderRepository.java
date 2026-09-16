package com.mdyaipay.payment.domain.withhold;

import java.util.Optional;

public interface WithholdOrderRepository {
    WithholdOrder save(WithholdOrder order);

    Optional<WithholdOrder> findByDeductionNo(String deductionNo);
}
