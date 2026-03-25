-- 贷款分期计划表
-- V15: 质押贷款分期还款计划

CREATE TABLE IF NOT EXISTS t_loan_installment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '分期计划唯一主键ID',
    loan_id BIGINT NOT NULL COMMENT '贷款ID',
    installment_no INT NOT NULL COMMENT '期号(1,2,3...)',
    due_date DATE NOT NULL COMMENT '到期日期',
    principal_amount DECIMAL(20,2) NOT NULL COMMENT '应还本金',
    interest_amount DECIMAL(20,2) NOT NULL COMMENT '应还利息',
    total_amount DECIMAL(20,2) NOT NULL COMMENT '应还总额',
    repaid_amount DECIMAL(20,2) DEFAULT 0 COMMENT '已还金额',
    status INT NOT NULL DEFAULT 1 COMMENT '1=待还,2=已还,3=逾期',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

    UNIQUE KEY uk_loan_installment (loan_id, installment_no),
    KEY idx_loan (loan_id),
    KEY idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='贷款分期计划表';
