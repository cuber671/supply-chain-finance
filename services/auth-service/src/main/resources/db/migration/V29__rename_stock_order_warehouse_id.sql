-- 重命名 warehouse_id 为 warehouse_ent_id，明确语义为仓储公司ID
ALTER TABLE t_stock_order CHANGE warehouse_id warehouse_ent_id BIGINT NOT NULL COMMENT '目标仓储公司ID（entId）';
