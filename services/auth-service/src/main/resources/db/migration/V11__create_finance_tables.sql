-- 金融模块数据库表
-- V11: 电子应收款项表、还款记录表

-- 电子应收款项表
CREATE TABLE IF NOT EXISTS t_receivable (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '账款记录唯一主键ID',
    receivable_no VARCHAR(64) NOT NULL COMMENT '账单编号：业务展示单号（如：AR20260220001）',
    business_scene INT NOT NULL COMMENT '来源场景：1-入库生成；2-转让配送签收生成',
    source_voucher_id BIGINT COMMENT '关联物流单ID：追溯产生这笔账的具体物流任务',
    creditor_ent_id BIGINT NOT NULL COMMENT '债权人ID：应收方企业（卖方/原货主）',
    debtor_ent_id BIGINT NOT NULL COMMENT '债务人ID：应付方企业（买方/借款企业）',
    initial_amount DECIMAL(20,2) NOT NULL COMMENT '原始金额：基于初始发货/入库数量计算的金额',
    adjusted_amount DECIMAL(20,2) NOT NULL COMMENT '结算金额：扣除物流损耗、拆分后的应收总额',
    collected_amount DECIMAL(20,2) NOT NULL DEFAULT 0 COMMENT '已回收金额：累计所有分批还款（现金+抵债）的总额',
    balance_unpaid DECIMAL(20,2) NOT NULL COMMENT '待还余额：结算金额 - 已回收金额',
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    due_date DATETIME NOT NULL COMMENT '最后还款日：逾期判定的基准时间',
    status INT NOT NULL DEFAULT 1 COMMENT '账款状态：1-待确认；2-生效中；3-部分还款；4-已结清；5-逾期',
    is_financed TINYINT NOT NULL DEFAULT 0 COMMENT '融资标识：该笔应收款是否已向银行申请融资',
    parent_id BIGINT COMMENT '父级ID：账款发生拆分时，指向原大额账单ID',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_receivable_no (receivable_no),
    KEY idx_creditor_ent_id (creditor_ent_id),
    KEY idx_debtor_ent_id (debtor_ent_id),
    KEY idx_source_voucher_id (source_voucher_id),
    KEY idx_status (status),
    KEY idx_due_date (due_date),
    KEY idx_business_scene (business_scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子应收款项表';

-- 还款记录表
CREATE TABLE IF NOT EXISTS t_repayment_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '还款记录唯一主键ID',
    receivable_id BIGINT NOT NULL COMMENT '关联应收款ID',
    repayment_no VARCHAR(64) NOT NULL COMMENT '还款编号：业务展示单号（如：REP20260220001）',
    repayment_type INT NOT NULL COMMENT '还款类型：1-现金还款；2-仓单抵债',
    amount DECIMAL(20,2) NOT NULL COMMENT '还款金额',
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    payment_voucher VARCHAR(128) COMMENT '付款凭证：现金还款时的转账凭证号',
    receipt_id BIGINT COMMENT '仓单ID：仓单抵债时关联的仓单ID',
    offset_price DECIMAL(20,2) COMMENT '抵债价格：仓单抵债时的评估价值',
    signature_hash VARCHAR(256) COMMENT '签名哈希：债务人数字签名',
    repayment_time DATETIME NOT NULL COMMENT '还款时间',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_repayment_no (repayment_no),
    KEY idx_receivable_id (receivable_id),
    KEY idx_repayment_type (repayment_type),
    KEY idx_repayment_time (repayment_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='还款记录表';
