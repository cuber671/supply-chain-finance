-- V24: 修改各表ID从bigint自增改为varchar(32)业务ID格式
-- 格式: {前缀}-{时间戳毫秒}-{4位随机数}

-- 清空所有相关表数据（按外键依赖顺序）
TRUNCATE TABLE t_logistics_track;
TRUNCATE TABLE t_logistics_assign_history;
TRUNCATE TABLE t_receipt_operation_log;
TRUNCATE TABLE t_receipt_endorsement;
TRUNCATE TABLE t_stock_order;
TRUNCATE TABLE t_warehouse_receipt;
TRUNCATE TABLE t_logistics_delegate;

-- 修改 t_stock_order: id 改为 VARCHAR(32)
ALTER TABLE t_stock_order MODIFY COLUMN id VARCHAR(32) NOT NULL COMMENT '入库单业务ID: SIO-{timestamp}-{random}';
ALTER TABLE t_stock_order DROP PRIMARY KEY;
ALTER TABLE t_stock_order ADD PRIMARY KEY (id);
ALTER TABLE t_stock_order MODIFY COLUMN receipt_id VARCHAR(32) COMMENT '关联仓单ID';

-- 修改 t_warehouse_receipt: id 改为 VARCHAR(32)
ALTER TABLE t_warehouse_receipt MODIFY COLUMN id VARCHAR(32) NOT NULL COMMENT '仓单业务ID: WR-{timestamp}-{random}';
ALTER TABLE t_warehouse_receipt DROP PRIMARY KEY;
ALTER TABLE t_warehouse_receipt ADD PRIMARY KEY (id);
ALTER TABLE t_warehouse_receipt MODIFY COLUMN parent_id VARCHAR(32) DEFAULT '0' COMMENT '父仓单ID';
ALTER TABLE t_warehouse_receipt MODIFY COLUMN root_id VARCHAR(32) DEFAULT '0' COMMENT '根仓单ID';
ALTER TABLE t_warehouse_receipt MODIFY COLUMN stock_order_id VARCHAR(32) COMMENT '关联入库单ID';

-- 修改 t_logistics_delegate: id 改为 VARCHAR(32)
ALTER TABLE t_logistics_delegate MODIFY COLUMN id VARCHAR(32) NOT NULL COMMENT '物流委托业务ID: LD-{timestamp}-{random}';
ALTER TABLE t_logistics_delegate DROP PRIMARY KEY;
ALTER TABLE t_logistics_delegate ADD PRIMARY KEY (id);
ALTER TABLE t_logistics_delegate MODIFY COLUMN receipt_id VARCHAR(32) COMMENT '关联仓单ID';

-- 修改 t_receivable: id 改为 VARCHAR(32)
ALTER TABLE t_receivable MODIFY COLUMN id VARCHAR(32) NOT NULL COMMENT '应收账款业务ID';
ALTER TABLE t_receivable DROP PRIMARY KEY;
ALTER TABLE t_receivable ADD PRIMARY KEY (id);

-- 修改 t_loan: id 改为 VARCHAR(32)
ALTER TABLE t_loan MODIFY COLUMN id VARCHAR(32) NOT NULL COMMENT '贷款业务ID';
ALTER TABLE t_loan DROP PRIMARY KEY;
ALTER TABLE t_loan ADD PRIMARY KEY (id);
ALTER TABLE t_loan MODIFY COLUMN receipt_id VARCHAR(32) COMMENT '关联仓单ID';

-- 修改 t_loan_repayment: id 改为 VARCHAR(32)
ALTER TABLE t_loan_repayment MODIFY COLUMN id VARCHAR(32) NOT NULL COMMENT '还款记录业务ID';
ALTER TABLE t_loan_repayment DROP PRIMARY KEY;
ALTER TABLE t_loan_repayment ADD PRIMARY KEY (id);
ALTER TABLE t_loan_repayment MODIFY COLUMN loan_id VARCHAR(32) COMMENT '关联贷款ID';