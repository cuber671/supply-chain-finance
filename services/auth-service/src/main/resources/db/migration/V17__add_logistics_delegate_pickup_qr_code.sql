-- =====================================================
-- Flyway Migration: V17__add_logistics_delegate_pickup_qr_code.sql
-- FISCO 供应链金融系统 - 物流委派单补充提货二维码字段
-- =====================================================
-- 修复审计发现：LogisticsDelegate.java 中定义了 pickupQrCode 字段
-- 但 t_logistics_delegate 表中缺少对应列，导致提货二维码无法持久化
-- =====================================================

ALTER TABLE t_logistics_delegate
    ADD COLUMN pickup_qr_code VARCHAR(128) COMMENT '提货二维码 - 提货授权码的二维码形式' AFTER auth_code;
