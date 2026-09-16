package com.mdyaipay.payment.domain.collect;

/**
 * 收单类产品形态：网银与快捷等同属「资金入账」链路，由 {@link PaymentGateway} 执行；
 * 代扣、代付使用独立聚合与网关接口。
 */
public enum PaymentProductType {
    /** 快捷/条码等实时收单（演示默认） */
    QUICK_COLLECTION,
    /** 网银：跳转银联网关或 B2C/B2B 网银，渠道多为异步回调确认 */
    ONLINE_BANKING
}
