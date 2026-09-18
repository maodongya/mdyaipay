package com.mdyaipay.payment.repository;

import com.mdyaipay.payment.domain.payout.PayoutOrder;

import java.util.Optional;

/**
 * 代付聚合持久化端口（按 {@code payoutNo} 幂等查询）。
 * <p>
 * <b>不负责</b>：渠道打款——见 {@link com.mdyaipay.payment.gateway.PayoutGateway}。
 */
public interface PayoutOrderRepository {

    /**
     * 按出款单号插入或覆盖。
     * <p>
     * 幂等：同一 {@code payoutNo} 再次保存更新已有行。
     */
    PayoutOrder save(PayoutOrder order);

    /**
     * 按出款单号点查；无行时 empty。无副作用。
     */
    Optional<PayoutOrder> findByPayoutNo(String payoutNo);
}
