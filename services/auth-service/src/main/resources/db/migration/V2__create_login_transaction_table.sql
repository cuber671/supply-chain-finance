-- V2: 登录事务表（用于企业登录TCC补偿机制）
-- 解决分布式环境下enterprise-service超时导致的数据不一致问题

CREATE TABLE IF NOT EXISTS t_login_transaction (
    tx_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '事务ID',
    tx_uuid VARCHAR(64) NOT NULL COMMENT '全局事务UUID（客户端生成）',
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    login_type VARCHAR(20) NOT NULL DEFAULT 'ENTERPRISE' COMMENT '登录类型: USER-用户, ENTERPRISE-企业',
    status VARCHAR(20) NOT NULL COMMENT '事务状态: TRYING-尝试中, CONFIRMED-已确认, CANCELLED-已取消, FAILED-失败',
    enterprise_ent_id BIGINT COMMENT '企业ID（登录成功后填充）',
    enterprise_session_id VARCHAR(255) COMMENT 'enterprise-service会话ID（登录成功后填充）',
    error_msg VARCHAR(500) COMMENT '错误信息（失败时填充）',
    try_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '尝试时间',
    confirm_time DATETIME COMMENT '确认时间',
    cancel_time DATETIME COMMENT '取消时间',
    expire_time DATETIME NOT NULL COMMENT '过期时间（用于清理）',
    PRIMARY KEY (tx_id),
    UNIQUE KEY uk_tx_uuid (tx_uuid),
    KEY idx_username (username),
    KEY idx_status (status),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录事务表（ TCC模式）';
