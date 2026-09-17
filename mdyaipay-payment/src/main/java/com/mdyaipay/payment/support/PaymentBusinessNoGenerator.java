package com.mdyaipay.payment.support;

import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import org.springframework.stereotype.Component;

/**
 * 支付域业务单号：未传入时使用 {@link SnowflakeIdGenerator} 生成全局唯一单号。
 */
@Component
public class PaymentBusinessNoGenerator {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public PaymentBusinessNoGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * @param clientProvided 调用方幂等单号；blank 时服务端雪花生成
     */
    public String resolveOrderNo(String clientProvided) {
        if (clientProvided != null && !clientProvided.isBlank()) {
            return clientProvided.trim();
        }
        return Long.toString(snowflakeIdGenerator.nextId());
    }

    public String resolveDeductionNo(String clientProvided) {
        return resolveOrderNo(clientProvided);
    }

    public String resolvePayoutNo(String clientProvided) {
        return resolveOrderNo(clientProvided);
    }
}
