package com.mdyaipay.payment.domain.withhold;

import java.util.Optional;

/**
 * 代扣聚合持久化端口（按 {@code deductionNo} 幂等查询）。
 */
public interface WithholdOrderRepository {
    WithholdOrder save(WithholdOrder order);

    Optional<WithholdOrder> findByDeductionNo(String deductionNo);
}
