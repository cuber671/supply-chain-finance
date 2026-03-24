-- V1__create_blockchain_transaction_record_table.sql
-- 链上交易审计记录表

CREATE TABLE IF NOT EXISTS `blockchain_transaction_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tx_hash` VARCHAR(128) NOT NULL COMMENT '区块链交易哈希',
    `jti` VARCHAR(64) DEFAULT NULL COMMENT 'JWT唯一标识',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
    `ent_id` BIGINT DEFAULT NULL COMMENT '企业ID',
    `blockchain_address` VARCHAR(64) DEFAULT NULL COMMENT '区块链地址',
    `operation` VARCHAR(32) NOT NULL COMMENT '操作类型',
    `contract_name` VARCHAR(64) DEFAULT NULL COMMENT '合约名称',
    `chain_id` VARCHAR(32) DEFAULT NULL COMMENT '链ID',
    `group_id` VARCHAR(32) DEFAULT NULL COMMENT '群组ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tx_hash` (`tx_hash`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_ent_id` (`ent_id`),
    KEY `idx_jti` (`jti`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='链上交易审计记录表';
