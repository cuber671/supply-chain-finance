-- =====================================================
-- Flyway Migration: V16__add_warehouse_receipt_missing_columns.sql
-- FISCO 供应链金融系统 - 仓单表补充缺失字段
-- =====================================================
-- 修复审计发现：WarehouseReceipt.java 中定义了 onChainStatus 和 version 字段
-- 但 t_warehouse_receipt 表中缺少对应列，导致：
--   1. onChainStatus 值无法持久化（永远为null）
--   2. @Version 乐观锁完全失效
-- =====================================================

-- 1. 添加上链状态字段
ALTER TABLE t_warehouse_receipt
    ADD COLUMN on_chain_status INT NOT NULL DEFAULT 0 COMMENT '上链状态: 0-待上链, 1-已上链, 2-上链失败';

-- 2. 添加乐观锁版本号字段（MyBatis-Plus @Version 依赖此列）
ALTER TABLE t_warehouse_receipt
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，用于并发更新控制';

-- 3. 为新字段创建索引（on_chain_status 查询频繁）
CREATE INDEX idx_warehouse_receipt_on_chain_status ON t_warehouse_receipt(on_chain_status);
