-- 添加仓单到入库单的溯源字段，确保一个入库单只出一个仓单
ALTER TABLE t_warehouse_receipt
ADD COLUMN stock_order_id BIGINT UNIQUE COMMENT '来源入库单ID';
