-- 已有库增量：CREATE TABLE IF NOT EXISTS 不会补列
ALTER TABLE payment_order ADD COLUMN merchant_id BIGINT NULL;
