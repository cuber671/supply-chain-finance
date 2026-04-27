-- V36: 添加贷款申请幂等性字段，支持幂等重复提交防护

-- 添加唯一请求ID字段，用于幂等性校验
ALTER TABLE t_loan ADD COLUMN idempotency_key VARCHAR(64) DEFAULT NULL COMMENT '幂等性校验Key（前端生成）';
ALTER TABLE t_loan ADD INDEX idx_idempotency_key (idempotency_key);

-- 添加请求来源追踪字段
ALTER TABLE t_loan ADD COLUMN request_source VARCHAR(32) DEFAULT NULL COMMENT '请求来源（如: WEB, APP, API）';