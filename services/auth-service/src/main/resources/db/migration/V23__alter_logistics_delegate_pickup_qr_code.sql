-- =====================================================
-- Flyway Migration: V23__alter_logistics_delegate_pickup_qr_code.sql
-- FISCO 供应链金融系统 - 扩大物流委派单提货二维码字段长度
-- =====================================================
-- 问题：generatePickupQrCode() 生成的 Base64 JSON 超过 128 字符
-- 解决：将 pickup_qr_code 从 VARCHAR(128) 扩大为 VARCHAR(512)
-- =====================================================

ALTER TABLE t_logistics_delegate
    MODIFY COLUMN pickup_qr_code VARCHAR(512) COMMENT '提货二维码 - 提货授权码的二维码形式';
