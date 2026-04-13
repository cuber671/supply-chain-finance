-- =====================================================
-- Flyway Migration: V2__blockchain_transaction_record.sql
-- FISCO 供应链金融系统 - 链上交易审计记录表
-- 用于关联 txHash 与 JWT 登录记录，实现链上行为审计回溯
-- =====================================================

-- 创建链上交易审计记录表
CREATE TABLE IF NOT EXISTS `blockchain_transaction_record` (
    `record_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tx_hash` VARCHAR(100) NOT NULL COMMENT '交易Hash',
    `contract_name` VARCHAR(50) NOT NULL COMMENT '合约名称',
    `method_name` VARCHAR(50) NOT NULL COMMENT '方法名称',
    `from_address` VARCHAR(100) DEFAULT NULL COMMENT '调用方地址',
    `to_address` VARCHAR(100) DEFAULT NULL COMMENT '目标地址',
    `input_data` TEXT DEFAULT NULL COMMENT '输入数据',
    `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
    `status` INT NOT NULL DEFAULT '0' COMMENT '状态: 0-pending, 1-success, 2-failed',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`record_id`),
    UNIQUE KEY `uk_tx_hash` (`tx_hash`),
    KEY `idx_contract_name` (`contract_name`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区块链交易记录表';
