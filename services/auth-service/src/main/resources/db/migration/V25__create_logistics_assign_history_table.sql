-- 物流指派历史记录表
-- 【P2-4修复】记录委派单的指派变更历史，防止无限次更换司机导致的管理混乱

CREATE TABLE IF NOT EXISTS t_logistics_assign_history (
    id              BIGINT           NOT NULL    COMMENT '主键ID',
    voucher_no      VARCHAR(64)      NOT NULL    COMMENT '委派单编号',
    driver_id       VARCHAR(64)      NOT NULL    COMMENT '司机ID',
    driver_name     VARCHAR(64)      NOT NULL    COMMENT '司机姓名',
    vehicle_no      VARCHAR(32)      NOT NULL    COMMENT '车牌号',
    assign_time     DATETIME         NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '指派时间',
    assign_type     INT              NOT NULL    COMMENT '指派类型：1=首次指派, 2=变更指派, 3=取消指派',
    PRIMARY KEY (id),
    INDEX idx_voucher_no (voucher_no),
    INDEX idx_assign_time (assign_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流指派历史记录表';