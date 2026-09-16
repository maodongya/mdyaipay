package com.mdyaipay.payment.service.collect;

import com.mdyaipay.payment.domain.collect.PaymentGateway;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentOrderRepository;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;
import com.mdyaipay.tools.timetrace.TimeTrace;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class PaymentApplicationService {
    private final PaymentOrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public PaymentApplicationService(PaymentOrderRepository orderRepository, PaymentGateway paymentGateway) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway must not be null");
    }

    @TimeTrace(value="createAndPay", reportOnComplete = true, reportThresholdMillis = 1000)
    public PaymentOrder createAndPay(String orderNo, long amount, String channel) {
        return createAndPay(orderNo, amount, channel, PaymentProductType.QUICK_COLLECTION);
    }

    /**
     * 发起收单：仅支持 {@link PaymentProductType#QUICK_COLLECTION} 与 {@link PaymentProductType#ONLINE_BANKING}。
     * 代扣、代付请使用对应应用服务。
     */
    public PaymentOrder createAndPay(String orderNo, long amount, String channel, PaymentProductType productType) {
        if (productType != PaymentProductType.QUICK_COLLECTION && productType != PaymentProductType.ONLINE_BANKING) {
            throw new IllegalArgumentException("productType must be QUICK_COLLECTION or ONLINE_BANKING");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        PaymentOrder existed = orderRepository.findByOrderNo(orderNo).orElse(null);
        if (existed != null) {
            return existed;
        }

        PaymentOrder order = new PaymentOrder(orderNo, amount, channel, productType);
        orderRepository.save(order);
        order.markProcessing();
        orderRepository.save(order);

        try {
            PaymentSubmitResult result = paymentGateway.pay(order);
            switch (result) {
                case SYNC_SUCCESS -> order.markSuccess();
                case SYNC_FAILURE -> order.markFailed();
                case AWAITING_CHANNEL_CONFIRMATION -> {
                    /* 网银等：保持 PROCESSING，等待回调或查单确认 */
                }
            }
        } catch (RuntimeException ex) {
            order.markFailed();
        }

        return orderRepository.save(order);
    }

    /**
     * 渠道异步确认（如网银支付结果通知）。仅允许 {@link PaymentProductType#ONLINE_BANKING} 且当前为 {@link PaymentStatus#PROCESSING} 的订单。
     */
    @TimeTrace(value="confirmChannelPayment", reportOnComplete = true, reportThresholdMillis = 1000)
    public PaymentOrder confirmChannelPayment(String orderNo, boolean success) {
        PaymentOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderNo));
        if (order.getProductType() != PaymentProductType.ONLINE_BANKING) {
            throw new IllegalStateException("channel confirm only for ONLINE_BANKING");
        }
        if (order.getStatus() != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("order not in PROCESSING: " + order.getStatus());
        }
        if (success) {
            order.markSuccess();
        } else {
            order.markFailed();
        }
        return orderRepository.save(order);
    }
    @TimeTrace(value="getPayment", reportOnComplete = true, reportThresholdMillis = 1000)
    public PaymentOrder getPayment(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderNo));
    }
}
