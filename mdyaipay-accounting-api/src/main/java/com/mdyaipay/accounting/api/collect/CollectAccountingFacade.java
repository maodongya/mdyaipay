package com.mdyaipay.accounting.api.collect;

import com.mdyaipay.tools.model.ApiResponse;

/**
 * 收单成功驱动账务入账的 Dubbo 门面。
 * <p>幂等：同一 {@link PaymentCollectSuccessCommand#getOrderNo()} 重复调用不重复加待结算。</p>
 */
public interface CollectAccountingFacade {

    /**
     * 收单 SUCCESS 后增加商户待结算（无 merchantId 时 no-op 仍返回 success）。
     * <p>前置条件：{@code amount > 0} 且 {@code orderNo} 非空；否则返回业务错误码。</p>
     */
    ApiResponse<Void> onPaymentCollectSuccess(PaymentCollectSuccessCommand command);
}
