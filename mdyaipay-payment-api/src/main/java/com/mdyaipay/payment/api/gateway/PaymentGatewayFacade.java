package com.mdyaipay.payment.api.gateway;

import com.mdyaipay.payment.api.gateway.command.ChannelConfirmCommand;
import com.mdyaipay.payment.api.gateway.command.CollectPaymentCommand;
import com.mdyaipay.payment.api.gateway.command.CreatePayoutCommand;
import com.mdyaipay.payment.api.gateway.command.CreateWithholdCommand;
import com.mdyaipay.payment.api.gateway.dto.PaymentOrderView;
import com.mdyaipay.payment.api.gateway.dto.PayoutOrderView;
import com.mdyaipay.payment.api.gateway.dto.WithholdOrderView;
import com.mdyaipay.tools.model.ApiResponse;

/**
 * 网关内网 Dubbo：收单/代扣/代付，替代 gateway→payment HTTP 转发。
 * <p>异常映射为 {@code ApiResponse.code != 0}，不抛业务异常到 Dubbo 边界。</p>
 */
public interface PaymentGatewayFacade {

    /**
     * 收单并调渠道。幂等：{@code orderNo} 已存在则返回已有单，不重复扣款。
     */
    ApiResponse<PaymentOrderView> collect(CollectPaymentCommand command);

    /** 按商户单号查收单快照。 */
    ApiResponse<PaymentOrderView> getPayment(String orderNo);

    /**
     * 渠道异步结果确认（如网银回调）。幂等：终态单重复确认不改变结果。
     */
    ApiResponse<PaymentOrderView> confirmChannelPayment(ChannelConfirmCommand command);

    /**
     * 代扣。幂等：{@code deductionNo} 已存在则返回已有单。
     */
    ApiResponse<WithholdOrderView> createWithhold(CreateWithholdCommand command);

    /**
     * 代付。幂等：{@code payoutNo} 已存在则返回已有单。
     */
    ApiResponse<PayoutOrderView> createPayout(CreatePayoutCommand command);
}
