-- 为 t_warehouse_receipt 表添加 remark 字段，支持仓单作废等功能
-- 注意：remark 字段可能已存在于其他位置（DROP后再ADD确保位置正确）

-- 如果 remark 列已存在，先删除 (MySQL不支持 DROP COLUMN IF EXISTS，使用存储过程判断)
DROP PROCEDURE IF EXISTS drop_column_if_exists;
DELIMITER //
CREATE PROCEDURE drop_column_if_exists()
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 't_warehouse_receipt'
        AND COLUMN_NAME = 'remark'
    ) THEN
        ALTER TABLE t_warehouse_receipt DROP COLUMN remark;
    END IF;
END //
DELIMITER ;
CALL drop_column_if_exists();
DROP PROCEDURE IF EXISTS drop_column_if_exists;

-- 在 status 列之后添加 remark 字段
ALTER TABLE t_warehouse_receipt ADD COLUMN remark VARCHAR(1000) COMMENT '备注' AFTER `status`;