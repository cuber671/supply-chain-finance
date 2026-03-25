-- 信用管理模块数据库表
-- V8: 企业信用档案表

-- 企业信用档案表
CREATE TABLE IF NOT EXISTS t_enterprise_credit_profile (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '信用记录ID',
    ent_id BIGINT NOT NULL COMMENT '企业ID，关联t_enterprise.ent_id',
    credit_score INT NOT NULL DEFAULT 800 COMMENT '信用评分：初始分800，范围300-1000',
    credit_level VARCHAR(10) NOT NULL COMMENT '信用等级：AAA, AA, A, B, C, D',
    available_limit DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '授信额度：平台允许该企业同时存在的未结清账款总额',
    used_limit DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '已用额度：当前未结清的应收款总和',
    overdue_count INT NOT NULL DEFAULT 0 COMMENT '逾期次数：累计历史违约次数',
    last_eval_time DATETIME NOT NULL COMMENT '上次评估时间：系统自动跑分的日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ent_id (ent_id),
    KEY idx_credit_level (credit_level),
    KEY idx_credit_score (credit_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业信用档案表';
