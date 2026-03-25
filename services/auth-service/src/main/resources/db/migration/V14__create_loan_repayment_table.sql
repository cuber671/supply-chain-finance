-- 贷款还款记录表
-- V14: 质押贷款还款记录

CREATE TABLE IF NOT EXISTS t_loan_repayment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '还款记录唯一主键ID',
    loan_id BIGINT NOT NULL COMMENT '贷款ID',
    repayment_no VARCHAR(64) NOT NULL COMMENT '还款编号：如REPLOAN20260321001',

    -- 还款金额
    principal_amount DECIMAL(20,2) NOT NULL COMMENT '本金金额',
    interest_amount DECIMAL(20,2) NOT NULL COMMENT '利息金额',
    penalty_amount DECIMAL(20,2) DEFAULT 0 COMMENT '罚息金额',
    total_amount DECIMAL(20,2) NOT NULL COMMENT '还款总金额',

    -- 还款方式
    repayment_type VARCHAR(20) NOT NULL COMMENT 'CASH/RECEIPT_OFFSET/RECEIVABLE_OFFSET/COLLATERAL_DISPOSAL/PARTIAL',

    -- 现金还款信息
    payment_voucher VARCHAR(128) COMMENT '付款凭证/流水号',
    payment_account VARCHAR(64) COMMENT '付款账户',

    -- 仓单抵债信息
    offset_receipt_id BIGINT COMMENT '仓单抵债时使用的仓单ID',
    offset_receipt_no VARCHAR(64) COMMENT '仓单编号',

    -- 应收账款抵债信息
    offset_receivable_id BIGINT COMMENT '应收账款抵债时使用的应收款ID',
    offset_receivable_no VARCHAR(64) COMMENT '应收款编号',

    -- 处置信息
    disposal_method VARCHAR(20) COMMENT '处置方式: AUCTION/DIRECT_SALE/WRITE_OFF',
    disposal_amount DECIMAL(20,2) COMMENT '处置金额',

    -- 签名与时间
    signature_hash VARCHAR(256) COMMENT '签名哈希',
    repayment_time DATETIME COMMENT '实际还款时间',

    -- 状态
    status INT NOT NULL DEFAULT 1 COMMENT '1=待确认,2=已确认,3=已拒绝',

    -- 区块链
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',

    -- 审计字段
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

    -- 索引
    UNIQUE KEY uk_repayment_no (repayment_no),
    KEY idx_loan (loan_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='贷款还款记录表';
