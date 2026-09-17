package com.mdyaipay.payment.service.withhold;

import com.mdyaipay.payment.domain.withhold.WithholdGateway;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.domain.withhold.WithholdOrderRepository;
import com.mdyaipay.payment.support.PaymentBusinessNoGenerator;
import com.mdyaipay.tools.timetrace.TimeTrace;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 代扣应用编排：校验、幂等、调 {@link WithholdGateway}、持久化。
 */
@Service
public class WithholdApplicationService {
    private final WithholdOrderRepository orderRepository;
    private final WithholdGateway withholdGateway;
    private final PaymentBusinessNoGenerator businessNoGenerator;

    public WithholdApplicationService(
            WithholdOrderRepository orderRepository,
            WithholdGateway withholdGateway,
            PaymentBusinessNoGenerator businessNoGenerator) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.withholdGateway = Objects.requireNonNull(withholdGateway, "withholdGateway must not be null");
        this.businessNoGenerator = Objects.requireNonNull(businessNoGenerator, "businessNoGenerator must not be null");
    }

    /**
     * 发起代扣。幂等：{@code deductionNo} 已存在则直接返回；blank 时服务端生成单号。
     */
    @TimeTrace(value = "WithholdApplicationService.createAndDeduct",reportThresholdMillis=100)
    public WithholdOrder createAndDeduct(String deductionNo, String agreementNo, long amount, String channel) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        deductionNo = businessNoGenerator.resolveDeductionNo(deductionNo);

        WithholdOrder existed = orderRepository.findByDeductionNo(deductionNo).orElse(null);
        if (existed != null) {
            return existed;
        }

        WithholdOrder order = new WithholdOrder(deductionNo, agreementNo, amount, channel);
        // orderRepository.save(order);
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
