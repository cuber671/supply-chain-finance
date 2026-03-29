-- 为 t_warehouse_receipt 表添加 remark 字段，支持仓单作废等功能
-- 注意：remark 字段可能已存在于其他位置（DROP后再ADD确保位置正确）

-- 如果 remark 列已存在，先删除
ALTER TABLE t_warehouse_receipt DROP COLUMN IF EXISTS remark;

-- 在 status 列之后添加 remark 字段
ALTER TABLE t_warehouse_receipt ADD COLUMN remark VARCHAR(1000) COMMENT '备注' AFTER `status`;