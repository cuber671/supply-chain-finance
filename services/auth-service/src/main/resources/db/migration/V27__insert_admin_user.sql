-- =====================================================
-- Flyway Migration: V27__insert_admin_user.sql
-- FISCO 供应链金融系统 - 创建系统管理员账号
-- =====================================================
-- 默认管理员账号：
--   用户名: admin
--   密码: 123456
-- =====================================================

INSERT INTO t_user (
    user_id,
    username,
    password,
    real_name,
    phone,
    email,
    enterprise_id,
    user_role,
    status,
    last_login_time,
    create_time,
    update_time
) VALUES (
    1000000000000000001,
    'admin',
    '$2a$10$ToBidmYMQ4SbiEECQdww2uVpGkNMCMFPbb3mCBtSq.vNboQ92zPqa',
    '系统管理员',
    '13800000000',
    'admin@fisco.com',
    0,
    'ADMIN',
    2,
    NULL,
    CURRENT_TIMESTAMP,
    NULL
) ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    real_name = VALUES(real_name),
    status = VALUES(status);
