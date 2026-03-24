-- ============================================================
-- FISCO BCOS 供应链金融平台 - MySQL 初始化脚本
-- ============================================================
-- 此脚本在 MySQL 容器首次启动时自动执行
-- 负责创建数据库和设置字符集
-- 注意：具体的表结构由各微服务的 Flyway 迁移管理

-- 创建数据库 (如果不存在)
CREATE DATABASE IF NOT EXISTS fisco_data
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE fisco_data;

-- 创建用户表 (auth-service)
CREATE TABLE IF NOT EXISTS t_user (
    user_id BIGINT NOT NULL COMMENT '用户ID，雪花算法生成',
    enterprise_id BIGINT NOT NULL COMMENT '所属企业ID',
    real_name VARCHAR(50) NOT NULL COMMENT '用户真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '企业邮箱',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码(BCrypt加密)',
    user_role VARCHAR(20) NOT NULL DEFAULT 'OPERATOR' COMMENT '职能角色: ADMIN-管理员, FINANCE-财务, OPERATOR-业务员',
    status INT NOT NULL DEFAULT 2 COMMENT '账户状态: 1-待审核, 2-正常, 3-冻结, 4-注销中, 5-已注销',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_email (email),
    KEY idx_enterprise_id (enterprise_id),
    KEY idx_status (status),
    KEY idx_user_role (user_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- 创建企业表 (enterprise-service)
CREATE TABLE IF NOT EXISTS enterprise (
    ent_id BIGINT NOT NULL COMMENT '企业ID',
    ent_name VARCHAR(100) NOT NULL COMMENT '企业名称',
    ent_role INT NOT NULL COMMENT '企业角色: 1-核心企业, 2-交易平台, 3-供应商, 6-金融机构, 9-仓储方, 12-物流方',
    org_code VARCHAR(50) NOT NULL COMMENT '组织机构代码',
    business_license VARCHAR(100) COMMENT '营业执照号',
    legal_person VARCHAR(50) COMMENT '法定代表人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    contact_email VARCHAR(100) COMMENT '联系邮箱',
    address VARCHAR(255) COMMENT '企业地址',
    credit_rating VARCHAR(10) COMMENT '信用评级',
    credit_limit BIGINT DEFAULT 0 COMMENT '授信额度(分)',
    status INT NOT NULL DEFAULT 0 COMMENT '企业状态: 0-待审核, 1-正常, 2-冻结, 3-注销中, 4-已注销',
    blockchain_address VARCHAR(100) COMMENT '区块链地址',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (ent_id),
    UNIQUE KEY uk_org_code (org_code),
    KEY idx_ent_role (ent_role),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业信息表';

-- 创建邀请码表
CREATE TABLE IF NOT EXISTS enterprise_invite_code (
    id BIGINT NOT NULL COMMENT '主键ID',
    ent_id BIGINT NOT NULL COMMENT '企业ID',
    invite_code VARCHAR(20) NOT NULL COMMENT '邀请码',
    invite_type INT NOT NULL COMMENT '邀请类型: 1-注册, 2-认证',
    used_status INT NOT NULL DEFAULT 0 COMMENT '使用状态: 0-未使用, 1-已使用',
    used_by BIGINT COMMENT '使用者ID',
    used_time DATETIME COMMENT '使用时间',
    expire_time DATETIME COMMENT '过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invite_code (invite_code),
    KEY idx_ent_id (ent_id),
    KEY idx_used_status (used_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业邀请码表';

-- 创建仓单表 (warehouse-service)
CREATE TABLE IF NOT EXISTS warehouse_receipt (
    receipt_id BIGINT NOT NULL COMMENT '仓单ID',
    receipt_no VARCHAR(50) NOT NULL COMMENT '仓单号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    owner_id BIGINT NOT NULL COMMENT '货主企业ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    goods_type VARCHAR(50) COMMENT '商品类型',
    total_weight DECIMAL(15,3) NOT NULL COMMENT '总重量(吨)',
    available_weight DECIMAL(15,3) NOT NULL COMMENT '可用重量(吨)',
    warehouse_address VARCHAR(255) COMMENT '仓库地址',
    storage_location VARCHAR(100) COMMENT '存储库位',
    status INT NOT NULL DEFAULT 1 COMMENT '仓单状态: 1-在库, 2-待转让, 3-已拆分/合并, 4-已核销, 5-物流转运中',
    receipt_hash VARCHAR(100) COMMENT '仓单区块链Hash',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (receipt_id),
    UNIQUE KEY uk_receipt_no (receipt_no),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_owner_id (owner_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单信息表';

-- 创建物流委托表 (logistics-service)
CREATE TABLE IF NOT EXISTS logistics_delegate (
    delegate_id BIGINT NOT NULL COMMENT '委托ID',
    delegate_no VARCHAR(50) NOT NULL COMMENT '委托单号',
    receipt_id BIGINT NOT NULL COMMENT '关联仓单ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    sender_id BIGINT NOT NULL COMMENT '发货方ID',
    receiver_id BIGINT NOT NULL COMMENT '收货方ID',
    logistics_company VARCHAR(100) COMMENT '物流公司',
    driver_name VARCHAR(50) COMMENT '司机姓名',
    driver_phone VARCHAR(20) COMMENT '司机电话',
    vehicle_no VARCHAR(20) COMMENT '车牌号',
    total_weight DECIMAL(15,3) NOT NULL COMMENT '总重量(吨)',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1-待指派, 2-已调度, 3-运输中, 4-已交付, 5-已失效',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (delegate_id),
    UNIQUE KEY uk_delegate_no (delegate_no),
    KEY idx_receipt_id (receipt_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流委托表';

-- 创建应收账款表 (finance-service)
CREATE TABLE IF NOT EXISTS receivable (
    receivable_id BIGINT NOT NULL COMMENT '应收账款ID',
    receivable_no VARCHAR(50) NOT NULL COMMENT '应收账款编号',
    debtor_id BIGINT NOT NULL COMMENT '债务方ID(核心企业)',
    creditor_id BIGINT NOT NULL COMMENT '债权方ID(供应商)',
    receipt_id BIGINT COMMENT '关联仓单ID',
    amount DECIMAL(15,2) NOT NULL COMMENT '应收账款金额',
    outstanding_amount DECIMAL(15,2) NOT NULL COMMENT '未还款金额',
    due_date DATE NOT NULL COMMENT '到期日期',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1-待确认, 2-生效中, 3-部分还款, 4-已结清, 5-逾期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (receivable_id),
    UNIQUE KEY uk_receivable_no (receivable_no),
    KEY idx_debtor_id (debtor_id),
    KEY idx_creditor_id (creditor_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收账款表';

-- 创建企业信用档案表 (credit-service)
CREATE TABLE IF NOT EXISTS enterprise_credit_profile (
    profile_id BIGINT NOT NULL COMMENT '档案ID',
    ent_id BIGINT NOT NULL COMMENT '企业ID',
    credit_score INT COMMENT '信用评分(0-1000)',
    credit_rating VARCHAR(10) COMMENT '信用评级',
    credit_limit BIGINT COMMENT '授信额度(分)',
    available_limit BIGINT COMMENT '可用额度(分)',
    overdue_count INT DEFAULT 0 COMMENT '逾期次数',
    total_transaction_amount DECIMAL(15,2) DEFAULT 0 COMMENT '累计交易金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (profile_id),
    UNIQUE KEY uk_ent_id (ent_id),
    KEY idx_credit_rating (credit_rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业信用档案表';

-- 创建区块链交易记录表 (fisco-gateway-service)
CREATE TABLE IF NOT EXISTS blockchain_transaction_record (
    record_id BIGINT NOT NULL COMMENT '记录ID',
    tx_hash VARCHAR(100) NOT NULL COMMENT '交易Hash',
    contract_name VARCHAR(50) NOT NULL COMMENT '合约名称',
    method_name VARCHAR(50) NOT NULL COMMENT '方法名称',
    from_address VARCHAR(100) COMMENT '调用方地址',
    to_address VARCHAR(100) COMMENT '目标地址',
    input_data TEXT COMMENT '输入数据',
    block_number BIGINT COMMENT '区块高度',
    status INT NOT NULL DEFAULT 0 COMMENT '状态: 0-pending, 1-success, 2-failed',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_tx_hash (tx_hash),
    KEY idx_contract_name (contract_name),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区块链交易记录表';

-- 创建索引以提高查询性能
CREATE INDEX idx_create_time ON t_user(create_time);
CREATE INDEX idx_create_time ON enterprise(create_time);
CREATE INDEX idx_create_time ON warehouse_receipt(create_time);
CREATE INDEX idx_create_time ON logistics_delegate(create_time);
CREATE INDEX idx_create_time ON receivable(create_time);
CREATE INDEX idx_create_time ON enterprise_credit_profile(create_time);
CREATE INDEX idx_create_time ON blockchain_transaction_record(create_time);
