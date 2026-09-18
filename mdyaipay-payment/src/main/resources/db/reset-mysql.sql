-- 清空 payment 域业务表（init-schema 且 reset-schema=true 时先执行，再跑 schema-mysql.sql）
DROP TABLE IF EXISTS payout_order;
DROP TABLE IF EXISTS withhold_order;
DROP TABLE IF EXISTS payment_order;
