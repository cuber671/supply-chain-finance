-- 仓单质押贷款主表
-- V13: 质押贷款核心表

CREATE TABLE IF NOT EXISTS t_loan (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '贷款记录唯一主键ID',
    loan_no VARCHAR(64) NOT NULL COMMENT '贷款编号：如LOAN20260321001',
    borrower_ent_id BIGINT NOT NULL COMMENT '借款企业ID',
    borrower_ent_name VARCHAR(128) COMMENT '借款企业名称(冗余存储)',
    finance_ent_id BIGINT NOT NULL COMMENT '金融机构ID(entRole=6)',
    finance_ent_name VARCHAR(128) COMMENT '金融机构名称(冗余存储)',
    receipt_id BIGINT NOT NULL COMMENT '质押仓单ID',
    receipt_no VARCHAR(64) COMMENT '仓单编号(冗余存储)',
    goods_name VARCHAR(100) COMMENT '货物名称(冗余存储)',
    warehouse_name VARCHAR(128) COMMENT '仓库名称(冗余存储)',

    -- 贷款金额信息
    applied_amount DECIMAL(20,2) NOT NULL COMMENT '申请金额',
    approved_amount DECIMAL(20,2) COMMENT '审批金额(审批后填充)',
    loan_amount DECIMAL(20,2) COMMENT '实际放款金额(放款后填充)',

    -- 利率与期限
    applied_interest_rate DECIMAL(5,4) COMMENT '申请利率',
    approved_interest_rate DECIMAL(5,4) COMMENT '审批利率',
    loan_interest_rate DECIMAL(5,4) COMMENT '实际执行利率',
    loan_days INT COMMENT '贷款期限(天)',

    -- 质押物评估
    collateral_value DECIMAL(20,2) NOT NULL COMMENT '抵押物评估价值',
    pledge_rate DECIMAL(5,4) DEFAULT 0.7000 COMMENT '质押率(默认70%)',
    max_loan_amount DECIMAL(20,2) COMMENT '最大可贷金额(抵押物×质押率)',

    -- 时间信息
    applied_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    approved_time DATETIME COMMENT '审批时间',
    disbursed_time DATETIME COMMENT '放款时间',
    loan_start_date DATE COMMENT '贷款起息日',
    loan_end_date DATE COMMENT '贷款到期日',

    -- 还款信息
    repaid_principal DECIMAL(20,2) DEFAULT 0 COMMENT '已还本金',
    repaid_interest DECIMAL(20,2) DEFAULT 0 COMMENT '已还利息',
    repaid_penalty DECIMAL(20,2) DEFAULT 0 COMMENT '已还罚息',
    outstanding_principal DECIMAL(20,2) COMMENT '剩余本金',
    outstanding_interest DECIMAL(20,2) COMMENT '应收利息',
    outstanding_penalty DECIMAL(20,2) DEFAULT 0 COMMENT '应收罚息',

    -- 状态与关联
    status INT NOT NULL DEFAULT 1 COMMENT '状态:1=待审批,2=已拒绝,3=已取消,4=待放款,5=已放款,6=还款中,7=已结清,8=逾期,9=已违约',
    receivable_id BIGINT COMMENT '关联应收款ID(可选,用于仓单质押+应收款融资组合场景)',

    -- 审批/拒绝信息
    approve_remark VARCHAR(512) COMMENT '审批备注',
    reject_reason VARCHAR(512) COMMENT '拒绝原因',
    reject_time DATETIME COMMENT '拒绝时间',
    cancel_reason VARCHAR(512) COMMENT '取消原因',
    cancel_time DATETIME COMMENT '取消时间',

    -- 区块链
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',

    -- 审计字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

    -- 索引
    UNIQUE KEY uk_loan_no (loan_no),
    KEY idx_borrower (borrower_ent_id),
    KEY idx_finance (finance_ent_id),
    KEY idx_receipt (receipt_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质押贷款主表';
