-- =====================================================
-- Flyway Migration: V19__create_login_transaction_table.sql
-- FISCO 供应链金融系统 - 登录事务表（TCC模式）
-- =====================================================
-- 修复审计发现：LoginTransaction.java 实体和 LoginTransactionMapper
-- 引用了 t_login_transaction 表，但整个 Flyway 迁移链中从未创建此表。
-- 这导致 EnterpriseLoginService 所有 TCC 操作在运行时抛出：
--   Table 'fisco_data.t_login_transaction' doesn't exist
-- =====================================================

CREATE TABLE IF NOT EXISTS `t_login_transaction` (
    `tx_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '事务ID',
    `tx_uuid` VARCHAR(64) NOT NULL COMMENT '事务唯一标识UUID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录用户名',
    `login_type` VARCHAR(20) NOT NULL COMMENT '登录类型: USER-用户登录, ENTERPRISE-企业登录',
    `status` VARCHAR(20) NOT NULL DEFAULT 'TRYING' COMMENT '事务状态: TRYING-尝试中, CONFIRMED-已确认, CANCELLED-已取消, FAILED-失败',
    `enterprise_ent_id` BIGINT DEFAULT NULL COMMENT '企业ID（企业登录时填充）',
    `enterprise_session_id` VARCHAR(128) DEFAULT NULL COMMENT '企业会话ID',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `try_time` DATETIME DEFAULT NULL COMMENT 'Try阶段时间',
    `confirm_time` DATETIME DEFAULT NULL COMMENT 'Confirm阶段时间',
    `cancel_time` DATETIME DEFAULT NULL COMMENT 'Cancel阶段时间',
    `expire_time` DATETIME DEFAULT NULL COMMENT '事务过期时间',
    PRIMARY KEY (`tx_id`),
    UNIQUE KEY `uk_tx_uuid` (`tx_uuid`),
    KEY `idx_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_enterprise_ent_id` (`enterprise_ent_id`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录事务表（TCC分布式事务追踪）';
