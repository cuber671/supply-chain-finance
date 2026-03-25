-- 仓单模块数据库表
-- V5: 仓单模块相关表

-- ==============================
-- 1. 仓库信息表
-- ==============================
CREATE TABLE IF NOT EXISTS t_warehouse (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
    ent_id BIGINT NOT NULL COMMENT '所属监管方企业ID',
    name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    address VARCHAR(255) NOT NULL COMMENT '仓库地址',
    contact_user VARCHAR(50) COMMENT '现场负责人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    status INT NOT NULL DEFAULT 1 COMMENT '仓库状态: 1-正常营业, 2-暂停接单, 3-已关闭',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    KEY idx_ent_id (ent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库信息表';

-- ==============================
-- 2. 电子仓单表
-- ==============================
CREATE TABLE IF NOT EXISTS t_warehouse_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '仓单ID',
    warehouse_id BIGINT NOT NULL COMMENT '物理仓库ID',
    on_chain_id VARCHAR(100) COMMENT '链上资产唯一标识(TokenID)',
    owner_ent_id BIGINT NOT NULL COMMENT '当前货权人企业ID',
    owner_user_id BIGINT NOT NULL COMMENT '当前操作人ID',
    warehouse_ent_id BIGINT NOT NULL COMMENT '监管方企业ID',
    warehouse_user_id BIGINT NOT NULL COMMENT '监管方操作人ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '货物名称',
    weight DECIMAL(18,4) NOT NULL COMMENT '货物重量/数量',
    unit VARCHAR(20) NOT NULL DEFAULT '吨' COMMENT '计量单位',
    parent_id BIGINT DEFAULT 0 COMMENT '父节点ID，用于记录拆分来源',
    root_id BIGINT DEFAULT 0 COMMENT '原始节点ID，用于全路径追溯',
    is_locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '质押锁定状态: 0-未锁定, 1-已锁定',
    status INT NOT NULL DEFAULT 1 COMMENT '仓单状态: 1-在库, 2-待转让, 3-已拆分/合并, 4-已核销, 5-物流转运中',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_on_chain_id (on_chain_id),
    KEY idx_owner_ent_id (owner_ent_id),
    KEY idx_warehouse_ent_id (warehouse_ent_id),
    KEY idx_parent_id (parent_id),
    KEY idx_root_id (root_id),
    KEY idx_is_locked (is_locked),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子仓单表';

-- ==============================
-- 3. 背书记录表
-- ==============================
CREATE TABLE IF NOT EXISTS t_receipt_endorsement (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '背书记录ID',
    receipt_id BIGINT NOT NULL COMMENT '关联仓单ID',
    transferor_ent_id BIGINT NOT NULL COMMENT '背书企业ID(转出方)',
    transferor_user_id BIGINT NOT NULL COMMENT '背书操作人ID',
    transferee_ent_id BIGINT NOT NULL COMMENT '被背书企业ID(接收方)',
    transferee_user_id BIGINT COMMENT '接收操作人ID',
    signature_hash VARCHAR(255) NOT NULL COMMENT '数字签名哈希',
    tx_hash VARCHAR(100) COMMENT '区块链交易哈希',
    status INT NOT NULL DEFAULT 1 COMMENT '记录状态: 1-待签收, 2-已签收, 3-已拒绝, 4-已撤回',
    remark VARCHAR(255) COMMENT '转让备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
    finish_time DATETIME COMMENT '完成时间',
    PRIMARY KEY (id),
    KEY idx_receipt_id (receipt_id),
    KEY idx_transferor_ent_id (transferor_ent_id),
    KEY idx_transferee_ent_id (transferee_ent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='背书记录表';

-- ==============================
-- 4. 仓单拆分/合并记录表
-- ==============================
CREATE TABLE IF NOT EXISTS t_receipt_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '操作记录ID',
    op_type INT NOT NULL COMMENT '操作类型: 1-拆分(Split), 2-合并(Merge)',
    source_receipt_ids VARCHAR(500) NOT NULL COMMENT '来源单据ID列表(逗号分隔)',
    target_receipt_ids VARCHAR(500) NOT NULL COMMENT '生成单据ID列表(逗号分隔)',
    total_weight DECIMAL(18,4) NOT NULL COMMENT '操作总重量',
    apply_ent_id BIGINT NOT NULL COMMENT '申请企业ID',
    apply_user_id BIGINT NOT NULL COMMENT '申请操作人ID',
    execute_ent_id BIGINT NOT NULL COMMENT '执行企业ID(仓储方)',
    execute_user_id BIGINT NOT NULL COMMENT '执行操作人ID',
    tx_hash VARCHAR(100) COMMENT '区块链交易哈希',
    status INT NOT NULL DEFAULT 1 COMMENT '记录状态: 1-待操作, 2-已完成, 3-已驳回',
    remark VARCHAR(255) COMMENT '操作备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    finish_time DATETIME COMMENT '完成时间',
    PRIMARY KEY (id),
    KEY idx_op_type (op_type),
    KEY idx_apply_ent_id (apply_ent_id),
    KEY idx_execute_ent_id (execute_ent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单拆分/合并记录表';

-- ==============================
-- 5. 入库单表
-- ==============================
CREATE TABLE IF NOT EXISTS t_stock_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    ent_id BIGINT NOT NULL COMMENT '申请企业ID',
    user_id BIGINT NOT NULL COMMENT '申请操作人ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '货物名称',
    weight DECIMAL(18,4) NOT NULL COMMENT '货物重量',
    unit VARCHAR(20) NOT NULL DEFAULT '吨' COMMENT '计量单位',
    attachment_url VARCHAR(500) COMMENT '附件URL(入库凭证)',
    status INT NOT NULL DEFAULT 1 COMMENT '入库单状态: 1-待审核, 2-已确认(可签发仓单), 3-已取消',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_ent_id (ent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库单表';
