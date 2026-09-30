package com.mdyaipay.accounting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 账务模块配置：数据源初始化、雪花 ID、MQ 消费开关。
 */
@ConfigurationProperties(prefix = "accounting")
public class AccountingProperties {

    private final Id id = new Id();
    private final Jdbc jdbc = new Jdbc();
    private final Mq mq = new Mq();

    public Id getId() {
        return id;
    }

    public Jdbc getJdbc() {
        return jdbc;
    }

    public Mq getMq() {
        return mq;
    }

    /** 雪花 ID 机器位。 */
    public static class Id {
        private long workerId = 4L;
        private long datacenterId = 1L;

        public long getWorkerId() {
            return workerId;
        }

        public void setWorkerId(long workerId) {
            this.workerId = workerId;
        }

        public long getDatacenterId() {
            return datacenterId;
        }

        public void setDatacenterId(long datacenterId) {
            this.datacenterId = datacenterId;
        }
    }

    /** JDBC 建表开关。 */
    public static class Jdbc {
        private boolean initSchema = true;
        private boolean resetSchema = false;

        public boolean isInitSchema() {
            return initSchema;
        }

        public void setInitSchema(boolean initSchema) {
            this.initSchema = initSchema;
        }

        public boolean isResetSchema() {
            return resetSchema;
        }

        public void setResetSchema(boolean resetSchema) {
            this.resetSchema = resetSchema;
        }
    }

    /** RocketMQ 消费配置。 */
    public static class Mq {
        private boolean enabled = false;
        private String paymentCollectConsumerGroup = "mdyaipay-accounting-payment-collect";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPaymentCollectConsumerGroup() {
            return paymentCollectConsumerGroup;
        }

        public void setPaymentCollectConsumerGroup(String paymentCollectConsumerGroup) {
            this.paymentCollectConsumerGroup = paymentCollectConsumerGroup;
        }
    }
}
