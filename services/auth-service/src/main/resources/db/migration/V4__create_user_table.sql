-- 用户（员工）管理模块数据库表
-- V4: 用户表

-- 用户表
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
