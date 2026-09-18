package com.mdyaipay.payment.service.payout;

import com.mdyaipay.payment.gateway.PayoutGateway;
import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.repository.PayoutOrderRepository;
import com.mdyaipay.payment.support.PaymentBusinessNoGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 代付应用编排：校验、幂等、调 {@link PayoutGateway}、持久化。
 * <p>
 * 写路径在 READ COMMITTED 事务内执行（InnoDB {@code payout_order}）。
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PayoutApplicationService {
    private final PayoutOrderRepository orderRepository;
    private final PayoutGateway payoutGateway;
    private final PaymentBusinessNoGenerator businessNoGenerator;

    public PayoutApplicationService(
            PayoutOrderRepository orderRepository,
            PayoutGateway payoutGateway,
            PaymentBusinessNoGenerator businessNoGenerator) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.payoutGateway = Objects.requireNonNull(payoutGateway, "payoutGateway must not be null");
        this.businessNoGenerator = Objects.requireNonNull(businessNoGenerator, "businessNoGenerator must not be null");
    }

    /**
     * 发起代付。幂等：{@code payoutNo} 已存在则直接返回；blank 时服务端生成单号。
     */
    public PayoutOrder createAndRemit(String payoutNo, long amount, String channel, String payeeRef) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        payoutNo = businessNoGenerator.resolvePayoutNo(payoutNo);

        PayoutOrder existed = orderRepository.findByPayoutNo(payoutNo).orElse(null);
        if (existed != null) {
            return existed;
        }

        PayoutOrder order = new PayoutOrder(payoutNo, amount, channel, payeeRef);
        order.markProcessing();
        orderRepository.save(order);

        try {
            boolean success = payoutGateway.remit(order);
            if (success) {
                order.markSuccess();
            } else {
                order.markFailed();
            }
        } catch (RuntimeException ex) {
            order.markFailed();
        }

        return orderRepository.save(order);
    }
}
