-- 清空账务域表（init-schema 且 reset-schema=true 时先执行）
DROP TABLE IF EXISTS merchant_wallet_txn_log;
DROP TABLE IF EXISTS merchant_wallet;
DROP TABLE IF EXISTS wallet_txn_log;
DROP TABLE IF EXISTS user_wallet;
