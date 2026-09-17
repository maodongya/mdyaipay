package com.mdyaipay.payment.testsupport;

import com.mdyaipay.payment.support.PaymentBusinessNoGenerator;
import com.mdyaipay.tools.id.SnowflakeIdGenerator;

public final class PaymentTestSupport {

    private PaymentTestSupport() {
    }

    public static PaymentBusinessNoGenerator businessNoGenerator() {
        return new PaymentBusinessNoGenerator(new SnowflakeIdGenerator(1, 1));
    }
}
