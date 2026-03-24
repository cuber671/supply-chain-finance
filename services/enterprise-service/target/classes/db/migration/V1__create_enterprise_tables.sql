-- 企业管理模块数据库表
-- V1: 企业表和邀请码表

-- 企业表
CREATE TABLE IF NOT EXISTS t_enterprise (
    ent_id BIGINT NOT NULL COMMENT '企业ID，雪花算法生成',
    enterprise_name VARCHAR(255) NOT NULL COMMENT '企业法定全称',
    org_code VARCHAR(18) NOT NULL COMMENT '统一社会信用代码',
    local_address VARCHAR(500) COMMENT '企业实际地址',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码(BCrypt加密)',
    pay_password VARCHAR(255) NOT NULL COMMENT '交易密码(BCrypt加密)',
    ent_role INT NOT NULL DEFAULT 1 COMMENT '业务角色: 1-核心企业, 2-现货交易平台, 3-供应商, 6-金融机构, 9-仓储方, 12-物流方',
    blockchain_address VARCHAR(66) COMMENT '区块链公开地址(0x开头)',
    encrypted_private_key TEXT COMMENT '加密存储的私钥(AES加密)',
    status INT NOT NULL DEFAULT 1 COMMENT '账户状态: 0-待审核, 1-正常, 2-冻结, 3-注销中, 4-已注销',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (ent_id),
    UNIQUE KEY uk_org_code (org_code),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status),
    KEY idx_blockchain_address (blockchain_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业信息表';

-- 企业邀请码表
CREATE TABLE IF NOT EXISTS t_invitation_code (
    code_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '邀请码ID',
    inviter_ent_id BIGINT NOT NULL COMMENT '邀请企业ID',
    code VARCHAR(10) NOT NULL COMMENT '邀请码字符串(6-10位)',
    max_uses INT NOT NULL DEFAULT 1 COMMENT '最大使用次数',
    used_count INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    expire_time DATETIME COMMENT '过期时间',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用, 2-已用罄',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (code_id),
    UNIQUE KEY uk_code (code),
    KEY idx_inviter_ent_id (inviter_ent_id),
    KEY idx_status (status),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业邀请码表';
