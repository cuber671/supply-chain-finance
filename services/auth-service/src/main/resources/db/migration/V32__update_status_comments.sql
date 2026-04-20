-- =====================================================
-- Flyway Migration: V32__update_status_comments.sql
-- FISCO 供应链金融系统 - 更新状态字段注释以反映实际支持的值
-- =====================================================
-- 同步数据库注释与实体类定义，确保文档一致性
-- =====================================================

-- 1. 更新仓单表状态注释（与 WarehouseReceipt.java 实体类保持一致）
ALTER TABLE t_warehouse_receipt
    MODIFY COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '仓单状态: 1-在库, 2-待转让, 3-已拆分/合并, 4-已核销, 5-物流转运中, 6-已作废, 7-待物流';

-- 2. 更新入库单表状态注释（与 StockOrder.java 实体类保持一致）
ALTER TABLE t_stock_order
    MODIFY COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '入库单状态: 1-待审核, 2-已确认入库, 3-已取消, 4-已完成出库, 5-转运中, 6-已核销';