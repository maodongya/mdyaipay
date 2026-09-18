package com.mdyaipay.payment.repository;

import com.mdyaipay.payment.domain.collect.PaymentOrder;

import java.util.Optional;

/**
 * 收单聚合持久化端口（按 {@code orderNo} 幂等查询）。
 * <p>
 * <b>不负责</b>：渠道受理、状态机——见 {@link com.mdyaipay.payment.gateway.PaymentGateway}、
 * {@link PaymentOrder}。
 */
public interface PaymentOrderRepository {

    /**
     * 按业务单号插入或覆盖。
     * <p>
     * 幂等：同一 {@code orderNo} 再次保存更新已有行，不新建第二笔。
     */
    PaymentOrder save(PaymentOrder order);

    /**
     * 按业务单号点查；无行时 empty。无副作用。
     */
    Optional<PaymentOrder> findByOrderNo(String orderNo);
}
