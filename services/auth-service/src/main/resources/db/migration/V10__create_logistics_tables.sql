-- 物流模块数据库表
-- V10: 电子物流委派单表

-- 电子物流委派单表
CREATE TABLE IF NOT EXISTS t_logistics_delegate (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '委派单唯一主键ID',
    voucher_no VARCHAR(64) NOT NULL COMMENT '业务展示编号（如：DPDO20260220001）',
    business_scene INT NOT NULL COMMENT '场景类型：1-直接移库；2-转让后移库；3-发货入库',
    receipt_id BIGINT COMMENT '关联原仓单ID：场景1和2必填',
    endorse_id BIGINT COMMENT '背书ID：场景2必填，用于核实买家身份',
    transport_quantity DECIMAL(20,4) NOT NULL COMMENT '本次运输数量：核心字段！记录本次拉走的具体数值',
    unit VARCHAR(20) NOT NULL COMMENT '计量单位：如"吨"、"千克"、"件"',
    owner_ent_id BIGINT NOT NULL COMMENT '授权企业：发起物流申请的货主/买方ID',
    carrier_ent_id BIGINT NOT NULL COMMENT '承运企业：指定的物流方公司ID',
    source_wh_id BIGINT NOT NULL COMMENT '起运地ID：货物目前存放的仓库',
    target_wh_id BIGINT COMMENT '目的地仓库ID：发往监管仓时必填',
    action_on_arrival INT NOT NULL DEFAULT 1 COMMENT '到货处理动作：1-生成新仓单；2-并入已有仓单',
    target_receipt_id BIGINT COMMENT '目标仓单ID：当执行增量入库时，指定并入哪张老单',
    driver_id VARCHAR(64) COMMENT '司机ID',
    driver_name VARCHAR(64) COMMENT '司机姓名',
    vehicle_no VARCHAR(32) COMMENT '车牌号',
    auth_code VARCHAR(128) COMMENT '提货授权码',
    auth_signature TEXT COMMENT '货主数字签名',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1-待指派, 2-已调度, 3-运输中, 4-已交付, 5-已失效',
    valid_until DATETIME NOT NULL COMMENT '凭证有效期',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_voucher_no (voucher_no),
    KEY idx_owner_ent_id (owner_ent_id),
    KEY idx_carrier_ent_id (carrier_ent_id),
    KEY idx_status (status),
    KEY idx_receipt_id (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子物流委派单表';

-- 物流轨迹记录表
CREATE TABLE IF NOT EXISTS t_logistics_track (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '轨迹记录ID',
    voucher_no VARCHAR(64) NOT NULL COMMENT '委派单编号',
    latitude DECIMAL(10,7) COMMENT '纬度',
    longitude DECIMAL(10,7) COMMENT '经度',
    location_name VARCHAR(256) COMMENT '位置名称',
    location_desc VARCHAR(512) COMMENT '位置描述',
    status INT NOT NULL COMMENT '状态：1-已提货, 2-运输中, 3-已到达',
    deviation_distance DECIMAL(10,2) COMMENT '偏离距离(米)',
    is_deviation TINYINT NOT NULL DEFAULT 0 COMMENT '是否偏航：0-否, 1-是',
    event_time DATETIME NOT NULL COMMENT '事件时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_voucher_no (voucher_no),
    KEY idx_event_time (event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流轨迹记录表';
