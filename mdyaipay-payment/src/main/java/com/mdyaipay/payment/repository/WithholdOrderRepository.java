package com.mdyaipay.payment.repository;

import com.mdyaipay.payment.domain.withhold.WithholdOrder;

import java.util.Optional;

/**
 * 代扣聚合持久化端口（按 {@code deductionNo} 幂等查询）。
 * <p>
 * <b>不负责</b>：渠道扣款——见 {@link com.mdyaipay.payment.gateway.WithholdGateway}。
 */
public interface WithholdOrderRepository {

    /**
     * 按代扣单号插入或覆盖。
     * <p>
     * 幂等：同一 {@code deductionNo} 再次保存更新已有行。
     */
    WithholdOrder save(WithholdOrder order);

    /**
     * 按代扣单号点查；无行时 empty。无副作用。
     */
    Optional<WithholdOrder> findByDeductionNo(String deductionNo);
}
