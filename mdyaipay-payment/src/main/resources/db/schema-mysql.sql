-- mdyaipay-payment MySQL schema (InnoDB, utf8mb4)
-- 金额单位：分（BIGINT）；时间：UTC TIMESTAMP(3)

CREATE TABLE IF NOT EXISTS payment_order (
    order_no      VARCHAR(64)  NOT NULL PRIMARY KEY,
    amount        BIGINT       NOT NULL,
    channel       VARCHAR(64)  NOT NULL,
    product_type  VARCHAR(32)  NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP(3) NOT NULL,
    updated_at    TIMESTAMP(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS withhold_order (
    deduction_no  VARCHAR(64)  NOT NULL PRIMARY KEY,
    agreement_no  VARCHAR(64)  NOT NULL,
    amount        BIGINT       NOT NULL,
    channel       VARCHAR(64)  NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP(3) NOT NULL,
    updated_at    TIMESTAMP(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payout_order (
    payout_no     VARCHAR(64)  NOT NULL PRIMARY KEY,
    amount        BIGINT       NOT NULL,
    channel       VARCHAR(64)  NOT NULL,
    payee_ref     VARCHAR(128) NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP(3) NOT NULL,
    updated_at    TIMESTAMP(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
