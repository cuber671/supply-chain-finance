-- 为入库单表增加实际仓库ID字段
-- 申请时warehouse_id存储仓储公司ID（entId），审核后actual_warehouse_id存储具体仓库ID（Warehouse.id）

ALTER TABLE t_stock_order ADD COLUMN actual_warehouse_id BIGINT DEFAULT 0 COMMENT '审核后填入的具体仓库ID';
