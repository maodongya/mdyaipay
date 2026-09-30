package com.mdyaipay.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 财务模块配置：雪花 ID 与建表开关。
 */
@ConfigurationProperties(prefix = "finance")
public class FinanceProperties {

    private final Id id = new Id();
    private final Jdbc jdbc = new Jdbc();

    /** 雪花 ID 配置。 */
    public Id getId() {
        return id;
    }

    /** JDBC 建表配置。 */
    public Jdbc getJdbc() {
        return jdbc;
    }

    /** 雪花 ID 机器位。 */
    public static class Id {
        private long workerId = 5L;
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
}
