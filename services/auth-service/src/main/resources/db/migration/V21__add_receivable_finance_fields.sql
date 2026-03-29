-- =====================================================
-- Flyway Migration: V21__add_receivable_finance_fields.sql
-- FISCO 供应链金融系统 - 应收款表添加融资字段
-- =====================================================
-- 为 t_receivable 表添加 finance_amount（融资金额）和 finance_ent_id（融资机构ID）字段
-- 对应实体类：Receivable.java 的 financeAmount 和 financeEntId 字段
--
-- 注意：finance-service 的 flyway.enabled=false，此迁移不会自动执行
-- 此脚本仅用于记录 schema 变更，人工执行或启用 flyway 后使用
-- =====================================================

ALTER TABLE t_receivable
    ADD COLUMN finance_amount DECIMAL(20,2) DEFAULT NULL AFTER is_financed,
    ADD COLUMN finance_ent_id BIGINT DEFAULT NULL AFTER finance_amount,
    ADD COLUMN signature VARCHAR(512) DEFAULT NULL AFTER finance_ent_id;

-- t_loan.finance_ent_id 改为允许 NULL（原为 NOT NULL，导致贷款申请失败）
ALTER TABLE t_loan MODIFY COLUMN finance_ent_id BIGINT DEFAULT NULL COMMENT '金融机构ID(entRole=6)';

-- t_loan 添加 disbursement_voucher 列（放款凭证号）
ALTER TABLE t_loan ADD COLUMN disbursement_voucher VARCHAR(128) DEFAULT NULL AFTER disbursement_time;
