-- 已有库增量（init-schema 时幂等；1060/1061 由 PaymentSchemaInitializer 忽略）

ALTER TABLE payment_order
    ADD COLUMN channel_trade_no VARCHAR(64) NULL COMMENT '渠道交易号，成功后写入' AFTER channel;
