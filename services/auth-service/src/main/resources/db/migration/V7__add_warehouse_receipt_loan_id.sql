-- 仓单添加质押贷款ID字段
-- 用于关联融资业务，记录质押时的贷款信息
-- 关联任务: 修复lockReceipt方法loanId参数未使用问题

ALTER TABLE t_warehouse_receipt
    ADD COLUMN loan_id VARCHAR(64) COMMENT '质押贷款ID - 关联融资业务' AFTER is_locked;

-- 创建索引用于快速查询质押中的仓单
ALTER TABLE t_warehouse_receipt
    ADD INDEX idx_loan_id (loan_id);
