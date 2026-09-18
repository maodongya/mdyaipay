package com.mdyaipay.payment.service.collect;

import com.mdyaipay.payment.gateway.PaymentGateway;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.repository.PaymentOrderRepository;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import com.mdyaipay.payment.domain.collect.PaymentSubmitResult;
import com.mdyaipay.payment.support.PaymentBusinessNoGenerator;
import com.mdyaipay.tools.timetrace.TimeTrace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 收单应用编排：校验、幂等、调 {@link PaymentGateway}、持久化。
 * <p>
 * 写路径在 READ COMMITTED 事务内执行（InnoDB {@code payment_order}），与连接池 {@code transaction-isolation} 一致。
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PaymentApplicationService {
    private final PaymentOrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentBusinessNoGenerator businessNoGenerator;

    public PaymentApplicationService(
            PaymentOrderRepository orderRepository,
            PaymentGateway paymentGateway,
            PaymentBusinessNoGenerator businessNoGenerator) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway must not be null");
        this.businessNoGenerator = Objects.requireNonNull(businessNoGenerator, "businessNoGenerator must not be null");
    }

    /** 快捷收单便捷入口，等价于 {@link PaymentProductType#QUICK_COLLECTION}。 */
    public PaymentOrder createAndPay(String orderNo, long amount, String channel) {
        return createAndPay(null, orderNo, amount, channel, PaymentProductType.QUICK_COLLECTION);
    }

    /** 指定产品类型的收单入口，{@code merchantId} 默认为 null。 */
    public PaymentOrder createAndPay(String orderNo, long amount, String channel, PaymentProductType productType) {
        return createAndPay(null, orderNo, amount, channel, productType);
    }

    /**
     * 发起收单：仅支持 {@link PaymentProductType#QUICK_COLLECTION} 与 {@link PaymentProductType#ONLINE_BANKING}。
     * 代扣、代付请使用对应应用服务。
     */
    @TimeTrace(value = "PaymentApplicationService.createAndPay",reportThresholdMillis=100)
    public PaymentOrder createAndPay(
            Long merchantId, String orderNo, long amount, String channel, PaymentProductType productType) {
        if (productType != PaymentProductType.QUICK_COLLECTION && productType != PaymentProductType.ONLINE_BANKING) {
            throw new IllegalArgumentException("productType must be QUICK_COLLECTION or ONLINE_BANKING");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        if (merchantId != null && merchantId <= 0) {
            throw new IllegalArgumentException("merchantId must be positive when present");
        }

        orderNo = businessNoGenerator.resolveOrderNo(orderNo);

        PaymentOrder existed = orderRepository.findByOrderNo(orderNo).orElse(null);
        if (existed != null) {
            return existed;
        }

        PaymentOrder order = new PaymentOrder(orderNo, amount, channel, productType, merchantId);
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

    /**
     * 按业务单号查询收单；无则抛 {@link IllegalArgumentException}。
     * <p>幂等：只读，无副作用。</p>
     */
    @Transactional(readOnly = true)
    public PaymentOrder getPayment(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderNo));
    }
}
