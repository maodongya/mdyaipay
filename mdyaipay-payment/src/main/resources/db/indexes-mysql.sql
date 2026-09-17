-- 已有库补建二级索引（init-schema 时幂等执行；索引已存在则忽略 1061）

CREATE INDEX idx_payment_order_status_created ON payment_order (status, created_at);
CREATE INDEX idx_payment_order_channel_status ON payment_order (channel, status);
CREATE INDEX idx_withhold_order_agreement_no ON withhold_order (agreement_no);
CREATE INDEX idx_withhold_order_status_created ON withhold_order (status, created_at);
CREATE INDEX idx_payout_order_status_created ON payout_order (status, created_at);
CREATE INDEX idx_payout_order_payee_ref ON payout_order (payee_ref);
