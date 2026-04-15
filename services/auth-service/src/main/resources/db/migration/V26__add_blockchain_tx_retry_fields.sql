-- V26: 为区块链交易记录表添加重试机制字段
-- 支持交易失败后的自动补偿

ALTER TABLE blockchain_transaction_record
    ADD COLUMN retry_count INT DEFAULT 0 COMMENT '重试次数',
    ADD COLUMN error_msg VARCHAR(500) COMMENT '最后错误信息',
    ADD COLUMN last_retry_time DATETIME COMMENT '最后重试时间';

-- 为现有记录初始化 retry_count 为 0
UPDATE blockchain_transaction_record SET retry_count = 0 WHERE retry_count IS NULL;

-- 将已完成的交易标记为成功状态
UPDATE blockchain_transaction_record SET status = 1 WHERE status IS NULL AND block_number IS NOT NULL;

-- 将未完成的交易标记为失败状态（可重试）
UPDATE blockchain_transaction_record SET status = 2, error_msg = '初始状态，待补偿任务处理' WHERE status IS NULL AND block_number IS NULL;