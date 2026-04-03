package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 登录事务实体 - TCC模式
 * 用于追踪企业登录的分布式事务状态
 */
@Data
@TableName("t_login_transaction")
public class LoginTransaction {

    public static final String STATUS_TRYING = "TRYING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String LOGIN_TYPE_USER = "USER";
    public static final String LOGIN_TYPE_ENTERPRISE = "ENTERPRISE";

    @TableId(type = IdType.AUTO)
    private Long txId;

    private String txUuid;

    @TableField(exist = true)
    private String username;

    private String loginType;

    private String status;

    private Long enterpriseEntId;

    private String enterpriseSessionId;

    private String errorMsg;

    private LocalDateTime tryTime;

    private LocalDateTime confirmTime;

    private LocalDateTime cancelTime;

    private LocalDateTime expireTime;
}
