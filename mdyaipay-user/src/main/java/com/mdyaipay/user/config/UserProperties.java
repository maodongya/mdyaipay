package com.mdyaipay.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户模块配置：数据源、雪花 ID、商户验签、AES 密钥、Outbox/MQ。
 */
@ConfigurationProperties(prefix = "user")
public class UserProperties {

    private final Jdbc jdbc = new Jdbc();
    private final Id id = new Id();
    private final Merchant merchant = new Merchant();
    private final Crypto crypto = new Crypto();
    private final Mq mq = new Mq();

    public Jdbc getJdbc() {
        return jdbc;
    }

    public Id getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public Mq getMq() {
        return mq;
    }

    public static class Jdbc {
        private boolean initSchema;
        /** true 时启动先 DROP 商户域表再建表（仅 MySQL）。 */
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

    /** 雪花 ID：worker / datacenter 在集群内唯一。 */
    public static class Id {
        private long workerId = 3L;
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

    public static class Merchant {
        private final Sign sign = new Sign();

        public Sign getSign() {
            return sign;
        }

        /** 商户开放 API 验签：允许的时间偏移（秒）。 */
        public static class Sign {
            private long maxSkewSeconds = 300L;

            public long getMaxSkewSeconds() {
                return maxSkewSeconds;
            }

            public void setMaxSkewSeconds(long maxSkewSeconds) {
                this.maxSkewSeconds = maxSkewSeconds;
            }
        }
    }

    /** 敏感字段 AES 密钥（Base64，解码后 32 字节），环境变量 {@code USER_AES_KEY}。 */
    public static class Crypto {
        private String aesKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

        public String getAesKey() {
            return aesKey;
        }

        public void setAesKey(String aesKey) {
            this.aesKey = aesKey;
        }
    }

    public static class Mq {
        /** 是否向 RocketMQ 投递 Outbox 消息。 */
        private boolean enabled = true;
        /** 是否启用 Outbox 定时扫描投递。 */
        private boolean outboxRelayEnabled = true;
        private String merchantTopic = "MDYAIPAY_USER_MERCHANT";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isOutboxRelayEnabled() {
            return outboxRelayEnabled;
        }

        public void setOutboxRelayEnabled(boolean outboxRelayEnabled) {
            this.outboxRelayEnabled = outboxRelayEnabled;
        }

        public String getMerchantTopic() {
            return merchantTopic;
        }

        public void setMerchantTopic(String merchantTopic) {
            this.merchantTopic = merchantTopic;
        }
    }
}
