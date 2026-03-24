-- 仓单服务数据库表结构
-- V1: 创建仓单相关表

-- 仓库表
CREATE TABLE IF NOT EXISTS t_warehouse (
    id BIGINT PRIMARY KEY COMMENT '仓库ID',
    ent_id BIGINT NOT NULL COMMENT '所属企业ID',
    name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    address VARCHAR(255) NOT NULL COMMENT '仓库地址',
    contact_user VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常, 0=停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_ent_id (ent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- 仓单表
CREATE TABLE IF NOT EXISTS t_warehouse_receipt (
    id BIGINT PRIMARY KEY COMMENT '仓单ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    on_chain_id VARCHAR(255) COMMENT '链上ID',
    owner_ent_id BIGINT NOT NULL COMMENT '持有人企业ID',
    owner_user_id BIGINT NOT NULL COMMENT '持有人用户ID',
    warehouse_ent_id BIGINT NOT NULL COMMENT '仓储企业ID',
    warehouse_user_id BIGINT NOT NULL COMMENT '仓储用户ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '货物名称',
    weight DECIMAL(20,4) NOT NULL COMMENT '重量',
    unit VARCHAR(20) NOT NULL COMMENT '计量单位',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父仓单ID',
    root_id BIGINT NOT NULL DEFAULT 0 COMMENT '根仓单ID',
    is_locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否质押锁定: 0=否, 1=是',
    loan_id VARCHAR(255) COMMENT '关联贷款ID',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1=在库, 2=待转让, 3=已拆分/合并, 4=已核销, 5=物流转运中',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner_ent_id (owner_ent_id),
    INDEX idx_on_chain_id (on_chain_id),
    INDEX idx_status (status),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_is_locked (is_locked),
    INDEX idx_parent_id (parent_id),
    INDEX idx_root_id (root_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓单表';

-- 入库单表
CREATE TABLE IF NOT EXISTS t_stock_order (
    id BIGINT PRIMARY KEY COMMENT '入库单ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    ent_id BIGINT NOT NULL COMMENT '申请企业ID',
    user_id BIGINT NOT NULL COMMENT '申请人用户ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '货物名称',
    weight DECIMAL(20,4) NOT NULL COMMENT '重量',
    unit VARCHAR(20) NOT NULL COMMENT '计量单位',
    attachment_url VARCHAR(500) COMMENT '附件URL',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1=待确认, 2=已确认, 3=已取消',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_ent_id (ent_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

-- 仓单背书记录表
CREATE TABLE IF NOT EXISTS t_receipt_endorsement (
    id BIGINT PRIMARY KEY COMMENT '背书记录ID',
    receipt_id BIGINT NOT NULL COMMENT '仓单ID',
    transferor_ent_id BIGINT NOT NULL COMMENT '转让方企业ID',
    transferor_user_id BIGINT NOT NULL COMMENT '转让方用户ID',
    transferee_ent_id BIGINT NOT NULL COMMENT '受让方企业ID',
    transferee_user_id BIGINT COMMENT '受让方用户ID',
    signature_hash VARCHAR(255) COMMENT '签名哈希',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1=待确认, 2=已接受, 3=已拒绝, 4=已撤回',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finish_time DATETIME COMMENT '完成时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_receipt_id (receipt_id),
    INDEX idx_transferor_ent_id (transferor_ent_id),
    INDEX idx_transferee_ent_id (transferee_ent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓单背书记录表';

-- 仓单操作记录表（拆分/合并）
CREATE TABLE IF NOT EXISTS t_receipt_operation_log (
    id BIGINT PRIMARY KEY COMMENT '操作记录ID',
    op_type INT NOT NULL COMMENT '操作类型: 1=拆分, 2=合并',
    source_receipt_ids VARCHAR(500) NOT NULL COMMENT '源仓单ID列表',
    target_receipt_ids VARCHAR(500) COMMENT '目标仓单ID列表',
    total_weight DECIMAL(20,4) NOT NULL COMMENT '总重量',
    apply_ent_id BIGINT NOT NULL COMMENT '申请企业ID',
    apply_user_id BIGINT NOT NULL COMMENT '申请用户ID',
    execute_ent_id BIGINT COMMENT '执行企业ID',
    execute_user_id BIGINT COMMENT '执行用户ID',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1=待执行, 2=已执行, 3=已驳回',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finish_time DATETIME COMMENT '完成时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_op_type (op_type),
    INDEX idx_apply_ent_id (apply_ent_id),
    INDEX idx_status (status),
    INDEX idx_source_receipt_ids (source_receipt_ids)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓单操作记录表';
