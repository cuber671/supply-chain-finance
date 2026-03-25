-- 信用管理模块 - 信用事件表
-- V9: 信用事件记录表

-- 信用事件记录表
CREATE TABLE IF NOT EXISTS t_credit_event (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '信用事件ID',
    ent_id BIGINT NOT NULL COMMENT '企业ID，关联t_enterprise.ent_id',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型：OVERDUE-逾期, DEFAULTER-违约, EARLY_REPAY-提前还款, ON_TIME_REPAY-准时还款, GOODS_UNDAMAGED-货物无损, STABLE_STORAGE-入库稳定',
    event_level VARCHAR(10) NOT NULL COMMENT '事件等级：LOW-低, MEDIUM-中, HIGH-高, SEVERE-严重',
    event_desc VARCHAR(500) COMMENT '事件描述',
    score_change INT NOT NULL DEFAULT 0 COMMENT '分值变化：正数为加分，负数为扣分',
    related_module VARCHAR(50) COMMENT '关联模块：WAREHOUSE-仓单, LOGISTICS-物流, FINANCE-金融',
    related_id VARCHAR(100) COMMENT '关联业务ID',
    chain_tx_hash VARCHAR(100) COMMENT '区块链交易哈希',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：0-已失效, 1-有效',
    report_time DATETIME NOT NULL COMMENT '事件上报时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    KEY idx_ent_id (ent_id),
    KEY idx_event_type (event_type),
    KEY idx_event_level (event_level),
    KEY idx_report_time (report_time),
    KEY idx_related_module (related_module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用事件记录表';
