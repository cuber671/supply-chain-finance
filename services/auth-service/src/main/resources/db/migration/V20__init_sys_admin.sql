-- =====================================================
-- Flyway Migration: V20__init_sys_admin.sql
-- FISCO 供应链金融系统 - 初始化超级管理员种子数据
-- =====================================================
-- 此脚本创建默认的系统管理员账号，用于：
-- 1. 私有化部署后的初始系统管理
-- 2. 自动化测试的认证锚点
--
-- 注意：由于 t_user.enterprise_id 列定义为 NOT NULL，
--       管理员用户使用 enterprise_id=0 表示"平台级用户"，
--       应用层需特殊处理 enterprise_id=0 的用户（不验证企业权限）
-- =====================================================

-- 修复说明 (V20.1)：
-- 原脚本使用 INSERT ... ON DUPLICATE KEY UPDATE 基于 user_id 判断重复，
-- 但 baseline 数据可能存在 user_id 不同但 username='admin' 的记录，
-- 导致 ON DUPLICATE KEY UPDATE 无法触发，密码未被正确同步。
-- 修复方案：先基于 username UPDATE 已存在记录，再 INSERT 兜底。

-- 先尝试更新已存在的 admin 用户（无论 user_id 是什么）
UPDATE t_user SET
    password = '$2a$10$ToBidmYMQ4SbiEECQdww2uVpGkNMCMFPbb3mCBtSq.vNboQ92zPqa',
    real_name = '系统管理员',
    status = 2,
    enterprise_id = 0
WHERE username = 'admin';

-- 如果没有匹配的行，则插入新记录
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
    1000000000000000001,  -- 固定ID，避免雪花算法冲突
    'admin',
    '$2a$10$ToBidmYMQ4SbiEECQdww2uVpGkNMCMFPbb3mCBtSq.vNboQ92zPqa',  -- BCrypt(123456)
    '系统管理员',
    '13800000000',
    'admin@fisco.com',
    0,                   -- 0=平台级用户，不属于任何企业
    'ADMIN',              -- 管理员角色
    2,                    -- 状态: 2=正常
    NULL,
    CURRENT_TIMESTAMP,
    NULL
) ON DUPLICATE KEY UPDATE
    password = VALUES(password);

-- =====================================================
-- 验证查询 (仅用于调试，生产环境可删除)
-- SELECT user_id, username, real_name, user_role, status FROM t_user WHERE username = 'admin';
-- =====================================================
