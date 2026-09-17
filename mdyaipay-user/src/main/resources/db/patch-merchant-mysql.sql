-- 增量：早期库可能缺少商户凭证表（CREATE TABLE IF NOT EXISTS 不会给已有库补表）
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
