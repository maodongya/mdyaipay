package com.mdyaipay.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 {@code payment.*} 配置（雪花 worker/datacenter、schema 开关等）。
 */
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private final Jdbc jdbc = new Jdbc();
    private final Id id = new Id();

    public Jdbc getJdbc() {
        return jdbc;
    }

    public Id getId() {
        return id;
    }

    public static class Jdbc {
        private boolean initSchema;
        /** true 时启动先 DROP payment 业务表再建表（仅 MySQL）。 */
        private boolean resetSchema;

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

    /** 雪花 ID：worker / datacenter 需在本集群内唯一。 */
    public static class Id {
        private long workerId = 1L;
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
}
