-- mdyaipay-payment MySQL schema (InnoDB, utf8mb4)
-- 金额单位：分（BIGINT）；时间：UTC TIMESTAMP(3)
--
-- 业务单号（order_no / deduction_no / payout_no）为主键，点查走 PK，无需重复单列索引。

CREATE TABLE IF NOT EXISTS payment_order (
    order_no      VARCHAR(64)  NOT NULL,
    merchant_id   BIGINT       NULL,
    amount        BIGINT       NOT NULL,
    channel       VARCHAR(64)  NOT NULL,
    product_type  VARCHAR(32)  NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP(3) NOT NULL,
    updated_at    TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (order_no),
    KEY idx_payment_order_status_created (status, created_at),
    KEY idx_payment_order_channel_status (channel, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS withhold_order (
    deduction_no  VARCHAR(64)  NOT NULL,
    agreement_no  VARCHAR(64)  NOT NULL,
    amount        BIGINT       NOT NULL,
    channel       VARCHAR(64)  NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP(3) NOT NULL,
    updated_at    TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (deduction_no),
    KEY idx_withhold_order_agreement_no (agreement_no),
    KEY idx_withhold_order_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payout_order (
    payout_no     VARCHAR(64)  NOT NULL,
    amount        BIGINT       NOT NULL,
    channel       VARCHAR(64)  NOT NULL,
    payee_ref     VARCHAR(128) NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP(3) NOT NULL,
    updated_at    TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (payout_no),
    KEY idx_payout_order_status_created (status, created_at),
    KEY idx_payout_order_payee_ref (payee_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
