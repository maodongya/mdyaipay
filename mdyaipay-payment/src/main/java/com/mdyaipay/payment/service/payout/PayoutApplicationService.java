package com.mdyaipay.payment.service.payout;

import com.mdyaipay.payment.domain.payout.PayoutGateway;
import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.domain.payout.PayoutOrderRepository;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class PayoutApplicationService {
    private final PayoutOrderRepository orderRepository;
    private final PayoutGateway payoutGateway;

    public PayoutApplicationService(PayoutOrderRepository orderRepository, PayoutGateway payoutGateway) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.payoutGateway = Objects.requireNonNull(payoutGateway, "payoutGateway must not be null");
    }

    public PayoutOrder createAndRemit(String payoutNo, long amount, String channel, String payeeRef) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        PayoutOrder existed = orderRepository.findByPayoutNo(payoutNo).orElse(null);
        if (existed != null) {
            return existed;
        }

        PayoutOrder order = new PayoutOrder(payoutNo, amount, channel, payeeRef);
        orderRepository.save(order);
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
