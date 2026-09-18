-- 清空商户域表（init-schema 且 reset-schema=true 时先执行）
DROP TABLE IF EXISTS merchant_api_credential;
DROP TABLE IF EXISTS merchant_member;
DROP TABLE IF EXISTS merchant_audit_record;
DROP TABLE IF EXISTS domain_outbox;
DROP TABLE IF EXISTS shop;
DROP TABLE IF EXISTS merchant;
