-- mdyaipay-finance MySQL schema (InnoDB, utf8mb4)
-- 金额单位：分（BIGINT）；业务日：yyyy-MM-dd

CREATE TABLE IF NOT EXISTS reconciliation_batch (
    id             BIGINT       NOT NULL COMMENT '雪花主键',
    channel        VARCHAR(64)  NOT NULL COMMENT '渠道编码',
    business_date  CHAR(10)     NOT NULL COMMENT '业务日 yyyy-MM-dd',
    status         VARCHAR(32)  NOT NULL COMMENT 'OPEN 或 PROCESSED',
    bill_source    VARCHAR(32)  NOT NULL COMMENT 'FILE 或 CHANNEL_PULL',
    matched_count  INT          NOT NULL COMMENT '平账笔数',
    created_at     TIMESTAMP(3) NOT NULL COMMENT '创建时间（UTC）',
    updated_at     TIMESTAMP(3) NOT NULL COMMENT '最后更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_recon_batch_channel_day (channel, business_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道对账批次';

CREATE TABLE IF NOT EXISTS reconciliation_difference (
    id                BIGINT       NOT NULL COMMENT '雪花主键',
    batch_id          BIGINT       NOT NULL COMMENT '批次主键',
    difference_type   VARCHAR(32)  NOT NULL COMMENT 'AMOUNT_MISMATCH、LOCAL_ONLY、CHANNEL_ONLY',
    channel_trade_no  VARCHAR(64)  NOT NULL COMMENT '渠道交易号',
    order_no          VARCHAR(64)  NULL     COMMENT '我方收单单号，渠道单边可空',
    local_amount      BIGINT       NULL     COMMENT '我方金额，单位：分',
    channel_amount    BIGINT       NULL     COMMENT '渠道金额，单位：分',
    PRIMARY KEY (id),
    KEY idx_recon_diff_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道对账差异';
