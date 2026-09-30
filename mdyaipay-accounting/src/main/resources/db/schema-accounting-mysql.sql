-- mdyaipay-accounting 资金域 MySQL schema (InnoDB, utf8mb4)

CREATE TABLE IF NOT EXISTS user_wallet (
    wallet_id      BIGINT       NOT NULL PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    balance        BIGINT       NOT NULL DEFAULT 0,
    frozen_amount  BIGINT       NOT NULL DEFAULT 0,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP(3) NOT NULL,
    updated_at     TIMESTAMP(3) NOT NULL,
    UNIQUE KEY uk_user_wallet_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wallet_txn_log (
    txn_id         BIGINT       NOT NULL PRIMARY KEY,
    wallet_id      BIGINT       NOT NULL,
    entry_type     VARCHAR(32)  NOT NULL,
    amount         BIGINT       NOT NULL,
    biz_key        VARCHAR(128) NOT NULL,
    balance_after  BIGINT       NOT NULL,
    frozen_after   BIGINT       NOT NULL,
    created_at     TIMESTAMP(3) NOT NULL,
    UNIQUE KEY uk_wallet_txn_biz (biz_key),
    KEY idx_wallet_txn_wallet (wallet_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS merchant_wallet (
    wallet_id               BIGINT       NOT NULL PRIMARY KEY,
    merchant_id             BIGINT       NOT NULL,
    available_amount        BIGINT       NOT NULL DEFAULT 0,
    pending_settle_amount   BIGINT       NOT NULL DEFAULT 0,
    withdrawable_amount     BIGINT       NOT NULL DEFAULT 0,
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_at              TIMESTAMP(3) NOT NULL,
    updated_at              TIMESTAMP(3) NOT NULL,
    UNIQUE KEY uk_merchant_wallet_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS merchant_wallet_txn_log (
    txn_id                  BIGINT       NOT NULL PRIMARY KEY,
    wallet_id               BIGINT       NOT NULL,
    entry_type              VARCHAR(32)  NOT NULL,
    amount                  BIGINT       NOT NULL,
    biz_key                 VARCHAR(128) NOT NULL,
    available_after         BIGINT       NOT NULL,
    pending_settle_after    BIGINT       NOT NULL,
    withdrawable_after      BIGINT       NOT NULL,
    created_at              TIMESTAMP(3) NOT NULL,
    UNIQUE KEY uk_merchant_wallet_txn_biz (biz_key),
    KEY idx_merchant_wallet_txn_wallet (wallet_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
