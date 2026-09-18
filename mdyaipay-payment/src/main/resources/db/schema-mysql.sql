-- mdyaipay-payment MySQL schema (InnoDB, utf8mb4)
-- 金额单位：分（BIGINT）；时间：UTC TIMESTAMP(3)
--
-- 主键 id：UUIDv7，存 BINARY(16)；业务单号唯一索引，供幂等点查。

CREATE TABLE IF NOT EXISTS payment_order (
    id            BINARY(16)   NOT NULL COMMENT 'UUIDv7 主键（16 字节）',
    order_no      VARCHAR(64)  NOT NULL COMMENT '收单业务单号，幂等键',
    merchant_id   BIGINT       NULL     COMMENT '商户 ID，可空',
    amount        BIGINT       NOT NULL COMMENT '订单金额，单位：分',
    channel       VARCHAR(64)  NOT NULL COMMENT '支付渠道编码',
    product_type  VARCHAR(32)  NOT NULL COMMENT '产品类型，如 QUICK_COLLECTION、ONLINE_BANKING',
    status        VARCHAR(32)  NOT NULL COMMENT '订单状态，如 PENDING、PROCESSING、SUCCESS、FAILED',
    created_at    TIMESTAMP(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at    TIMESTAMP(3) NOT NULL COMMENT '最后更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收单订单表';

CREATE TABLE IF NOT EXISTS withhold_order (
    id            BINARY(16)   NOT NULL COMMENT 'UUIDv7 主键（16 字节）',
    deduction_no  VARCHAR(64)  NOT NULL COMMENT '代扣业务单号，幂等键',
    agreement_no  VARCHAR(64)  NOT NULL COMMENT '代扣协议号',
    amount        BIGINT       NOT NULL COMMENT '扣款金额，单位：分',
    channel       VARCHAR(64)  NOT NULL COMMENT '代扣渠道编码',
    status        VARCHAR(32)  NOT NULL COMMENT '订单状态，如 PENDING、PROCESSING、SUCCESS、FAILED',
    created_at    TIMESTAMP(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at    TIMESTAMP(3) NOT NULL COMMENT '最后更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_withhold_order_deduction_no (deduction_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代扣订单表';

CREATE TABLE IF NOT EXISTS payout_order (
    id            BINARY(16)   NOT NULL COMMENT 'UUIDv7 主键（16 字节）',
    payout_no     VARCHAR(64)  NOT NULL COMMENT '出款业务单号，幂等键',
    amount        BIGINT       NOT NULL COMMENT '出款金额，单位：分',
    channel       VARCHAR(64)  NOT NULL COMMENT '出款渠道编码',
    payee_ref     VARCHAR(128) NOT NULL COMMENT '收款方标识（商户侧引用）',
    status        VARCHAR(32)  NOT NULL COMMENT '订单状态，如 PENDING、PROCESSING、SUCCESS、FAILED',
    created_at    TIMESTAMP(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at    TIMESTAMP(3) NOT NULL COMMENT '最后更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payout_order_payout_no (payout_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出款订单表';
