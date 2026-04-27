-- V34: 修改更多关联字段从bigint改为varchar(32)以匹配主表ID类型

-- 修改 t_receipt_endorsement: receipt_id 改为 VARCHAR(32)
ALTER TABLE t_receipt_endorsement MODIFY COLUMN receipt_id VARCHAR(32) NOT NULL COMMENT '关联仓单ID';

-- 修改 t_loan_installment: id, loan_id 改为 VARCHAR(32)
ALTER TABLE t_loan_installment MODIFY COLUMN id VARCHAR(32) NOT NULL COMMENT '分期还款记录ID';
ALTER TABLE t_loan_installment DROP PRIMARY KEY;
ALTER TABLE t_loan_installment ADD PRIMARY KEY (id);
ALTER TABLE t_loan_installment MODIFY COLUMN loan_id VARCHAR(32) NOT NULL COMMENT '关联贷款ID';

-- 修改 t_repayment_record: receipt_id 改为 VARCHAR(32)
ALTER TABLE t_repayment_record MODIFY COLUMN receipt_id VARCHAR(32) COMMENT '仓单ID：仓单抵债时关联的仓单ID';