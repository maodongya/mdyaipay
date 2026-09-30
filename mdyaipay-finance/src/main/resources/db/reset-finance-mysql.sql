-- 清空财务对账表后重建。先删差异再删批次。

DROP TABLE IF EXISTS reconciliation_difference;
DROP TABLE IF EXISTS reconciliation_batch;
