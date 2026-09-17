-- mdyaipay-user 商户域 MySQL schema (InnoDB, utf8mb4)

CREATE TABLE IF NOT EXISTS merchant (
    merchant_id   BIGINT       NOT NULL PRIMARY KEY,
    name          VARCHAR(128) NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMP(3) NOT NULL,
    updated_at    TIMESTAMP(3) NOT NULL,
    KEY idx_merchant_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shop (
    shop_id            BIGINT       NOT NULL PRIMARY KEY,
    merchant_id        BIGINT       NOT NULL,
    shop_name          VARCHAR(128) NOT NULL,
    category           VARCHAR(64)  NULL,
    operating_status   VARCHAR(16)  NOT NULL,
    created_at         TIMESTAMP(3) NOT NULL,
    updated_at         TIMESTAMP(3) NOT NULL,
    KEY idx_shop_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS merchant_audit_record (
    record_id     BIGINT       NOT NULL PRIMARY KEY,
    merchant_id   BIGINT       NOT NULL,
    auditor       VARCHAR(64)  NOT NULL,
    result        VARCHAR(16)  NOT NULL,
    remark        VARCHAR(512) NULL,
    created_at    TIMESTAMP(3) NOT NULL,
    KEY idx_merchant_audit_merchant (merchant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS merchant_member (
    member_id     BIGINT       NOT NULL PRIMARY KEY,
    merchant_id   BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    created_at    TIMESTAMP(3) NOT NULL,
    UNIQUE KEY uk_merchant_member (merchant_id, user_id),
    KEY idx_merchant_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商户开放 API 凭证表：签发时写入；secret 仅存 secret_cipher（AES），明文仅签发响应一次
CREATE TABLE IF NOT EXISTS merchant_api_credential (
    credential_id   BIGINT       NOT NULL PRIMARY KEY,
    merchant_id     BIGINT       NOT NULL,
    app_key         VARCHAR(64)  NOT NULL,
    secret_cipher   VARCHAR(512) NOT NULL COMMENT 'AES-GCM Base64 密文',
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMP(3) NOT NULL,
    updated_at      TIMESTAMP(3) NOT NULL,
    UNIQUE KEY uk_merchant_app_key (app_key),
    KEY idx_merchant_credential_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS domain_outbox (
    outbox_id       BIGINT       NOT NULL PRIMARY KEY,
    aggregate_type  VARCHAR(32)  NOT NULL,
    aggregate_id    BIGINT       NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMP(3) NOT NULL,
    sent_at         TIMESTAMP(3) NULL,
    KEY idx_outbox_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
