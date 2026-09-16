package com.mdyaipay.payment.service.withhold;

import com.mdyaipay.payment.domain.withhold.WithholdGateway;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.domain.withhold.WithholdOrderRepository;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class WithholdApplicationService {
    private final WithholdOrderRepository orderRepository;
    private final WithholdGateway withholdGateway;

    public WithholdApplicationService(WithholdOrderRepository orderRepository, WithholdGateway withholdGateway) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.withholdGateway = Objects.requireNonNull(withholdGateway, "withholdGateway must not be null");
    }

    public WithholdOrder createAndDeduct(String deductionNo, String agreementNo, long amount, String channel) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        WithholdOrder existed = orderRepository.findByDeductionNo(deductionNo).orElse(null);
        if (existed != null) {
            return existed;
        }

        WithholdOrder order = new WithholdOrder(deductionNo, agreementNo, amount, channel);
        orderRepository.save(order);
        order.markProcessing();
        orderRepository.save(order);

        try {
            boolean success = withholdGateway.deduct(order);
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
