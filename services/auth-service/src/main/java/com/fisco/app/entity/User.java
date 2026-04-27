package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 用户（员工）实体类
 *
 * 对应数据库表: t_user
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_user")
public class User {

    /**
     * 用户ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long userId;

    /**
     * 所属企业ID
     */
    @TableField("enterprise_id")
    private Long enterpriseId;

    /**
     * 用户真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 企业邮箱
     */
    private String email;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 登录密码 (BCrypt加密)
     */
    private String password;

    /**
     * 职能角色: ADMIN-管理员, FINANCE-财务, OPERATOR-业务员
     */
    @TableField("user_role")
    private String userRole;

    /**
     * 账户状态: 1-待审核, 2-正常, 3-冻结, 4-注销中, 5-已注销
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 状态常量
     */
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_NORMAL = 2;
    public static final int STATUS_FROZEN = 3;
    public static final int STATUS_CANCELLING = 4;
    public static final int STATUS_CANCELLED = 5;
    public static final int STATUS_PENDING_CANCEL = 6;

    /**
     * 角色常量
     */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_FINANCE = "FINANCE";
    public static final String ROLE_OPERATOR = "OPERATOR";
}
