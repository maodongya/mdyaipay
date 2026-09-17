package com.mdyaipay.payment.api.dubbo;

import com.mdyaipay.payment.api.gateway.PaymentGatewayFacade;
import com.mdyaipay.payment.api.gateway.command.ChannelConfirmCommand;
import com.mdyaipay.payment.api.gateway.command.CollectPaymentCommand;
import com.mdyaipay.payment.api.gateway.command.CreatePayoutCommand;
import com.mdyaipay.payment.api.gateway.command.CreateWithholdCommand;
import com.mdyaipay.payment.api.gateway.dto.PaymentOrderView;
import com.mdyaipay.payment.api.gateway.dto.PayoutOrderView;
import com.mdyaipay.payment.api.gateway.dto.WithholdOrderView;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.payout.PayoutOrder;
import com.mdyaipay.payment.domain.withhold.WithholdOrder;
import com.mdyaipay.payment.service.collect.PaymentApplicationService;
import com.mdyaipay.payment.service.payout.PayoutApplicationService;
import com.mdyaipay.payment.service.withhold.WithholdApplicationService;
import com.mdyaipay.tools.exception.ErrorCode;
import com.mdyaipay.tools.model.ApiResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * {@link PaymentGatewayFacade} Dubbo 实现：委托各应用服务编排，领域规则不在此类重复。
 * <p>不负责商户 Open API 验签解密（gateway 完成后再传入明文 Command）。</p>
 */
@Component
@DubboService(version = "1.0.0")
public class PaymentGatewayFacadeImpl implements PaymentGatewayFacade {

    private final PaymentApplicationService paymentService;
    private final WithholdApplicationService withholdService;
    private final PayoutApplicationService payoutService;

    public PaymentGatewayFacadeImpl(
            PaymentApplicationService paymentService,
            WithholdApplicationService withholdService,
            PayoutApplicationService payoutService) {
        this.paymentService = paymentService;
        this.withholdService = withholdService;
        this.payoutService = payoutService;
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<PaymentOrderView> collect(CollectPaymentCommand command) {
        return run(() -> toView(paymentService.createAndPay(
                command.getMerchantId(),
                command.getOrderNo(),
                command.getAmount(),
                command.getChannel(),
                parseProductType(command.getProductType()))));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<PaymentOrderView> getPayment(String orderNo) {
        return run(() -> toView(paymentService.getPayment(orderNo)));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<PaymentOrderView> confirmChannelPayment(ChannelConfirmCommand command) {
        return run(() -> toView(paymentService.confirmChannelPayment(command.getOrderNo(), command.isSuccess())));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<WithholdOrderView> createWithhold(CreateWithholdCommand command) {
        return run(() -> toView(withholdService.createAndDeduct(
                command.getDeductionNo(),
                command.getAgreementNo(),
                command.getAmount(),
                command.getChannel())));
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<PayoutOrderView> createPayout(CreatePayoutCommand command) {
        return run(() -> toView(payoutService.createAndRemit(
                command.getPayoutNo(),
                command.getAmount(),
                command.getChannel(),
                command.getPayeeRef())));
    }

    /** 空 productType 默认快捷收单，与 HTTP 时代行为一致。 */
    private static PaymentProductType parseProductType(String raw) {
        if (raw == null || raw.isBlank()) {
            return PaymentProductType.QUICK_COLLECTION;
        }
        return PaymentProductType.valueOf(raw);
    }

    private static PaymentOrderView toView(PaymentOrder order) {
        return new PaymentOrderView(
                order.getOrderNo(),
                order.getMerchantId(),
                order.getAmount(),
                order.getChannel(),
                order.getProductType().name(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private static WithholdOrderView toView(WithholdOrder order) {
        return new WithholdOrderView(
                order.getDeductionNo(),
                order.getAgreementNo(),
                order.getAmount(),
                order.getChannel(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private static PayoutOrderView toView(PayoutOrder order) {
        return new PayoutOrderView(
                order.getPayoutNo(),
                order.getAmount(),
                order.getChannel(),
                order.getPayeeRef(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    /**
     * 将应用层异常映射为 {@code ApiResponse}，避免 Dubbo 边界抛未检异常。
     */
    private static <T> ApiResponse<T> run(Callable<T> action) {
        try {
            return ApiResponse.ok(action.call());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(), ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface Callable<T> {
        T call();
    }
}
