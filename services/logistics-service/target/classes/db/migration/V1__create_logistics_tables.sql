-- 物流服务数据库表结构
-- V1: 创建物流相关表

-- 物流委派单表
CREATE TABLE IF NOT EXISTS t_logistics_delegate (
    id BIGINT PRIMARY KEY COMMENT '委派单ID',
    voucher_no VARCHAR(50) NOT NULL COMMENT '委派单编号',
    business_scene INT NOT NULL COMMENT '场景类型: 1-直接移库, 2-转让后移库, 3-发货入库',
    receipt_id BIGINT COMMENT '关联原仓单ID',
    endorse_id BIGINT COMMENT '背书ID',
    transport_quantity DECIMAL(20,4) NOT NULL COMMENT '运输数量',
    unit VARCHAR(20) NOT NULL COMMENT '计量单位',
    owner_ent_id BIGINT NOT NULL COMMENT '授权企业/货主ID',
    carrier_ent_id BIGINT NOT NULL COMMENT '承运企业ID',
    source_wh_id BIGINT COMMENT '起运地仓库ID',
    target_wh_id BIGINT COMMENT '目的地仓库ID',
    action_on_arrival INT COMMENT '到货处理动作: 1-生成新仓单, 2-并入已有仓单',
    target_receipt_id BIGINT COMMENT '目标仓单ID',
    driver_id VARCHAR(50) COMMENT '司机ID',
    driver_name VARCHAR(50) COMMENT '司机姓名',
    vehicle_no VARCHAR(20) COMMENT '车牌号',
    auth_code VARCHAR(10) COMMENT '提货授权码',
    pickup_qr_code TEXT COMMENT '提货二维码',
    auth_signature TEXT COMMENT '货主数字签名',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1-待指派, 2-已调度, 3-运输中, 4-已交付, 5-已失效',
    valid_until DATETIME COMMENT '凭证有效期',
    chain_tx_hash VARCHAR(100) COMMENT '区块链交易哈希',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_voucher_no (voucher_no),
    INDEX idx_owner_ent_id (owner_ent_id),
    INDEX idx_carrier_ent_id (carrier_ent_id),
    INDEX idx_status (status),
    INDEX idx_receipt_id (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流委派单表';

-- 物流轨迹记录表
CREATE TABLE IF NOT EXISTS t_logistics_track (
    id BIGINT PRIMARY KEY COMMENT '轨迹记录ID',
    voucher_no VARCHAR(50) NOT NULL COMMENT '委派单编号',
    latitude DECIMAL(10,6) COMMENT '纬度',
    longitude DECIMAL(10,6) COMMENT '经度',
    location_name VARCHAR(100) COMMENT '位置名称',
    location_desc VARCHAR(255) COMMENT '位置描述',
    status INT DEFAULT 1 COMMENT '状态: 1-已提货, 2-运输中, 3-已到达',
    deviation_distance DECIMAL(10,2) COMMENT '偏离距离(米)',
    is_deviation TINYINT(1) DEFAULT 0 COMMENT '是否偏航: 0-否, 1-是',
    event_time DATETIME COMMENT '事件时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_voucher_no (voucher_no),
    INDEX idx_event_time (event_time),
    INDEX idx_is_deviation (is_deviation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹记录表';
