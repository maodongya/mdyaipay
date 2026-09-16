package com.mdyaipay.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private final Jdbc jdbc = new Jdbc();

    public Jdbc getJdbc() {
        return jdbc;
    }

    public static class Jdbc {
        private boolean initSchema;

        public boolean isInitSchema() {
            return initSchema;
        }

        public void setInitSchema(boolean initSchema) {
            this.initSchema = initSchema;
        }
    }
}
