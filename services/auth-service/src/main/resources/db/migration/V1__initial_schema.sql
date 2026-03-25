-- =====================================================
-- Flyway Migration: V1__initial_schema.sql
-- FISCO 供应链金融系统 - 初始数据库结构
-- =====================================================

-- 创建用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `enterprise_id` BIGINT DEFAULT NULL COMMENT '关联企业ID',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色: ADMIN, USER, ENTERPRISE',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 创建企业表
CREATE TABLE IF NOT EXISTS `enterprise` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `enterprise_name` VARCHAR(100) NOT NULL COMMENT '企业名称',
    `credit_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
    `legal_person` VARCHAR(50) DEFAULT NULL COMMENT '法定代表人',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `registered_capital` DECIMAL(15,2) DEFAULT NULL COMMENT '注册资本',
    `business_status` TINYINT DEFAULT 1 COMMENT '经营状态: 0-注销, 1-正常',
    `credit_score` INT DEFAULT 100 COMMENT '信用评分',
    `risk_level` TINYINT DEFAULT 1 COMMENT '风险等级: 1-低, 2-中, 3-高',
    `on_chain_status` TINYINT DEFAULT 0 COMMENT '上链状态: 0-未上链, 1-已上链',
    `chain_address` VARCHAR(100) DEFAULT NULL COMMENT '区块链地址',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_credit_code` (`credit_code`),
    KEY `idx_enterprise_name` (`enterprise_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业信息表';

-- 创建操作日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    `operation` VARCHAR(50) DEFAULT NULL COMMENT '操作类型',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
    `request_params` TEXT COMMENT '请求参数',
    `response_result` TEXT COMMENT '响应结果',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `execution_time` BIGINT DEFAULT NULL COMMENT '执行时间(毫秒)',
    `status_code` INT DEFAULT NULL COMMENT 'HTTP状态码',
    `error_message` TEXT COMMENT '错误信息',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 创建 Flyway 版本记录表（如果不存在）
CREATE TABLE IF NOT EXISTS `flyway_schema_history` (
    `installed_rank` INT NOT NULL,
    `version` VARCHAR(50) DEFAULT NULL,
    `description` VARCHAR(200) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `script` VARCHAR(1000) NOT NULL,
    `checksum` INT DEFAULT NULL,
    `installed_by` VARCHAR(100) NOT NULL,
    `installed_on` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `execution_time` INT NOT NULL,
    `success` BOOL NOT NULL,
    PRIMARY KEY (`installed_rank`),
    KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
