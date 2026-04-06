-- 物流偏航信用扣分记录表
-- 用于记录偏航检测时信用扣分失败的待补偿记录
-- SC-005-01 修复

CREATE TABLE IF NOT EXISTS t_logistics_deviation_credit_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ent_id BIGINT COMMENT '企业ID',
    logistics_order_id VARCHAR(64) COMMENT '物流订单号',
    deviation_level INT COMMENT '偏航级别',
    deviation_desc VARCHAR(255) COMMENT '偏航描述',
    status INT DEFAULT 0 COMMENT '状态: 0=PENDING, 1=SUCCESS, 2=FAILED',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    error_msg VARCHAR(500) COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_retry_time DATETIME COMMENT '最后重试时间',
    INDEX idx_ent_id (ent_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流偏航信用扣分记录表';