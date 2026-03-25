-- =====================================================
-- Flyway Migration: V18__add_stock_order_receipt_id.sql
-- FISCO 供应链金融系统 - 入库单补充receipt_id关联字段
-- =====================================================
-- 修复审计发现：StockOrder.java 中 receiptId 字段是核心业务关联键
-- 用于 confirmBurn 时将出库单与原仓单进行一对一关联，避免用 goodsName
-- 匹配导致的错误关联问题（代码中有 FIX 注释说明）。
-- t_stock_order 表缺少 receipt_id 列，导致关联查询失效。
-- =====================================================

ALTER TABLE t_stock_order
    ADD COLUMN receipt_id BIGINT COMMENT '关联仓单ID - 记录此入库单由哪个仓单生成，用于confirmBurn关联核销' AFTER user_id;

-- 为 receipt_id 创建索引（关联查询频繁）
CREATE INDEX idx_stock_order_receipt_id ON t_stock_order(receipt_id);
